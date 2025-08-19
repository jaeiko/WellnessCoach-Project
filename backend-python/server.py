# backend-python/server.py

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import Optional, Dict, Any
import json

# main.py에서 ConversationManager 클래스와 root_agent를 가져옵니다.
from main import ConversationManager
from multi_tool_agent.agent import root_agent

# --- 데이터 모델 정의 ---
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
manager: Optional[ConversationManager] = None

@app.on_event("startup")
async def startup_event():
    """서버가 시작될 때 AI ConversationManager를 초기화합니다."""
    global manager
    manager = ConversationManager(agent=root_agent)
    await manager.initialize()
    print("🤖 FastAPI 서버와 AI 코치가 준비되었습니다.")

@app.get("/")
def read_root():
    """서버 상태 확인용 기본 경로입니다."""
    return {"status": "WellnessCoach AI Server is running"}

@app.post("/chat", response_model=ChatResponse)
async def handle_chat(request: ChatRequest):
    """
    안드로이드 앱의 모든 요청을 처리하는 메인 API 엔드포인트입니다.
    """
    # 1. 서버가 준비되었는지 확인합니다.
    if not manager:
        raise HTTPException(status_code=503, detail="AI Manager is not initialized")

    print(f"Received data from Android: {request.dict()}")

    # 2. main.py에 있는 API 전용 함수를 호출하여 AI의 응답을 받습니다.
    ai_raw_response = await manager.send_message_for_api(request.message, request.healthData)
    
    chat_text = ""
    notification_payload = None

    # 3. AI의 응답이 JSON 형식(초기 분석)인지, 일반 텍스트(코칭 대화)인지 확인하고 처리합니다.
    try:
        # AI 응답을 JSON으로 파싱 시도
        response_data = json.loads(ai_raw_response)
        
        # 성공하면, 사용자에게 보여줄 텍스트(response_for_user)를 추출합니다.
        chat_text = response_data.get("response_for_user", "분석 결과를 해석할 수 없습니다.")
        
        # [🚨 위험 요소]가 분석 결과에 포함되어 있는지 확인하여 알림을 생성합니다.
        if "[🚨 위험 요소]" in chat_text:
            if "수면" in chat_text:
                notification_payload = NotificationPayload(title="수면 부족 경고", body="어젯밤 수면의 질이 좋지 않았습니다. 앱에서 확인해보세요.")
            elif "스트레스" in chat_text:
                 notification_payload = NotificationPayload(title="높은 스트레스 감지", body="스트레스 지수가 높게 측정되었습니다. 휴식이 필요합니다.")
            # (향후 걸음 수 목표 달성, 혈압 등 다른 조건들도 추가할 수 있습니다.)

    except json.JSONDecodeError:
        # JSON 파싱에 실패하면, 일반 텍스트 응답으로 간주합니다.
        chat_text = ai_raw_response

    # 4. 최종적으로 안드로이드 앱에 전달할 응답을 ChatResponse 형식에 맞춰 구성합니다.
    return ChatResponse(
        chatResponse=chat_text,
        notification=notification_payload
    )