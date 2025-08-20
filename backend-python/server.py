# server.py (최종 수정본)
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import Optional, Dict, Any
import json
import asyncio
import shlex
import subprocess
import os
from dotenv import load_dotenv
import google.generativeai as genai

# 🔽 [핵심 추가] 서버가 시작되기 전에 .env 파일을 로드하고 API 키를 설정합니다.
load_dotenv(dotenv_path="multi_tool_agent/.env")
genai.configure(api_key=os.getenv("GOOGLE_AI_API_KEY"))

# 필요한 모듈들을 직접 가져옵니다.
from multi_tool_agent.agent import root_agent
from firebase_utils import initialize_firebase, get_conversation_history, save_conversation_turn
from google.adk.runners import Runner
from google.adk.sessions import InMemorySessionService
from google.genai.types import Content, Part

# --- 데이터 모델 정의 (기존과 동일) ---
class NotificationPayload(BaseModel):
    title: str
    body: str
class ChatRequest(BaseModel):
    userId: str
    sessionId: str
    message: str
    healthData: Optional[Dict[str, Any]] = None
class ChatResponse(BaseModel):
    chatResponse: str
    notification: Optional[NotificationPayload] = None

# --- FastAPI 앱 설정 ---
app = FastAPI()
db = initialize_firebase()
session_service = InMemorySessionService()
runner = Runner(agent=root_agent, app_name="wellness_coach_app", session_service=session_service)

@app.on_event("startup")
async def startup_event():
    await session_service.create_session(app_name="wellness_coach_app", user_id="user_1", session_id="session_12345")
    print("🤖 FastAPI 서버와 AI 코치가 준비되었습니다.")

@app.get("/")
def read_root():
    return {"status": "WellnessCoach AI Server is running"}

@app.post("/chat", response_model=ChatResponse)
async def handle_chat(request: ChatRequest):
    if not runner:
        raise HTTPException(status_code=503, detail="AI Runner is not initialized")

    print(f"Received data from Android: {request.dict()}")

    try:
        with open("prompts/analytics_prompt.txt", "r", encoding="utf-8") as f:
            prompt_template = f.read()
    except FileNotFoundError:
        raise HTTPException(status_code=500, detail="Prompt file not found")

    # [핵심] 프롬프트 구성
    history_list = get_conversation_history(db, request.userId)
    final_prompt = prompt_template.replace("((CONVERSATION_HISTORY))", "\n".join(history_list))
    if request.healthData:
        final_prompt = final_prompt.replace("((USER_GOAL))", request.message)
        final_prompt = final_prompt.replace("((USER_PROFILE))", json.dumps(request.healthData.get("user_profile", {}), ensure_ascii=False))
        final_prompt = final_prompt.replace("((TIMESERIES_DATA))", json.dumps(request.healthData.get("timeseries_data", []), ensure_ascii=False))
        final_prompt = final_prompt.replace("((SLEEP_DATA))", json.dumps(request.healthData.get("sleep_data", {}), ensure_ascii=False))
        final_prompt = final_prompt.replace("((EXERCISE_DATA))", json.dumps(request.healthData.get("exercise_data", []), ensure_ascii=False))
        final_prompt = final_prompt.replace("((NUTRITION_DATA))", json.dumps(request.healthData.get("nutrition_data", {}), ensure_ascii=False))
        final_prompt = final_prompt.replace("((VITALS_DATA))", json.dumps(request.healthData.get("vitals_data", {}), ensure_ascii=False))
    
    full_query = f"{final_prompt}\n\nLatest User Query: {request.message}"
    
    # [핵심] ADK 실행 및 Tool-Use 피드백 루프
    final_response_text = "죄송합니다. 답변을 생성하는 데 실패했습니다."
    async for event in runner.run_async(
        user_id=request.userId,
        session_id=request.sessionId,
        new_message=Content(parts=[Part(text=full_query)])
    ):
        # 🔽 [핵심 수정] event.is_tool_code() 대신, event.type을 직접 확인합니다.
        if event.type == "TOOL_CODE":
            tool_code = event.content.parts[0].text
            print(f"🤖 AI가 도구를 사용하려고 합니다: {tool_code}")
            
            process = await asyncio.create_subprocess_shell(
                f"python -c \"{tool_code}\"",
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
            )
            stdout, stderr = await process.communicate()
            tool_output = stdout.decode().strip()
            if stderr:
                print(f"🛠️ 도구 실행 중 오류 발생: {stderr.decode().strip()}")
                tool_output = f"오류: {stderr.decode().strip()}"

            print(f"🛠️ 도구 실행 결과: {tool_output}")
            
            await runner.provide_tool_response(
                user_id=request.userId,
                session_id=request.sessionId,
                tool_response=tool_output,
            )
        # 🔽 [핵심 수정] event.is_final_response() 대신, event.type을 직접 확인합니다.
        elif event.type == "FINAL_RESPONSE":
            final_response_text = event.content.parts[0].text
            break

    save_conversation_turn(db, request.userId, request.sessionId, request.message, final_response_text)
    
    chat_text = final_response_text
    notification_payload = None
    try:
        response_data = json.loads(final_response_text)
        chat_text = response_data.get("response_for_user", "Error parsing JSON.")
        if "[🚨 위험 요소]" in chat_text:
            if "수면" in chat_text:
                notification_payload = NotificationPayload(title="수면 부족 경고", body="어젯밤 수면의 질이 좋지 않았습니다.")
            elif "스트레스" in chat_text:
                notification_payload = NotificationPayload(title="높은 스트레스 감지", body="스트레스 지수가 높게 측정되었습니다.")
    except json.JSONDecodeError:
        pass

    return ChatResponse(chatResponse=chat_text, notification=notification_payload)