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
    if not manager:
        raise HTTPException(status_code=503, detail="AI Manager is not initialized")

    print(f"Received data from Android: {request.dict()}")

    ai_raw_response = await manager.send_message_for_api(request.message, request.healthData)
    
    # 🔽 [핵심 수정] AI 응답 처리 로직을 더 명확하게 변경합니다.
    chat_text_for_user = "" 
    notification_payload = None

    try:
        # AI 응답이 JSON 형식인지 먼저 확인합니다. (데이터 분석 결과)
        response_data = json.loads(ai_raw_response)
        chat_text_for_user = response_data.get("response_for_user", "오류: 분석 결과는 받았지만, 사용자 메시지를 찾을 수 없습니다.")

        # [🚨 위험 요소]가 있는지 확인하여 알림 생성
        if "[🚨 위험 요소]" in chat_text_for_user:
            # ... (기존 알림 로직은 그대로 유지)
            if "수면" in chat_text_for_user:
                notification_payload = NotificationPayload(title="수면 부족 경고", body="어젯밤 수면의 질이 좋지 않았습니다. 앱에서 확인해보세요.")
            elif "스트레스" in chat_text_for_user:
                 notification_payload = NotificationPayload(title="높은 스트레스 감지", body="스트레스 지수가 높게 측정되었습니다. 휴식이 필요합니다.")

    except json.JSONDecodeError:
        # JSON 파싱에 실패했다면, AI가 일반 텍스트로 응답한 것으로 간주합니다. (코칭 또는 도구 사용 결과)
        chat_text_for_user = ai_raw_response.strip() # 혹시 모를 공백 제거
    except Exception as e:
        # 기타 예외 처리
        print(f"Error processing AI response: {e}")
        chat_text_for_user = "AI 응답을 처리하는 중 오류가 발생했습니다."

    # 최종적으로 안드로이드 앱에 전달할 응답을 구성합니다.
    return ChatResponse(
        chatResponse=chat_text_for_user, # 사용자에게 보여줄 최종 텍스트
        notification=notification_payload
    )
