# backend-python/server.py

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import uvicorn
from typing import Optional, Dict, Any
import json

# main.py에서 ConversationManager 클래스와 root_agent를 가져옵니다.
from main import ConversationManager
from multi_tool_agent.agent import root_agent

# [수정] firebase_utils와 util 파일에서 필요한 함수들을 모두 가져옵니다.
from firebase_utils import update_user_status
from util import is_data_sufficient, get_health_questionnaire


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

    # "분석" 요청 시 데이터 충분성 검사 로직은 유지합니다.
    if "분석" in request.message or "분석해줘" in request.message:
        if not is_data_sufficient(request.healthData):
            questionnaire = get_health_questionnaire()
            # [핵심 추가] 설문지를 보내는 동시에, 사용자의 상태를 '설문 답변 대기중'으로 변경
            update_user_status(manager.db, request.userId, "AWAITING_SURVEY_RESPONSE")
            print(f"🔄 데이터 부족으로 설문지 전송. 사용자 상태를 'AWAITING_SURVEY_RESPONSE'로 변경.")
            return ChatResponse(chatResponse=questionnaire)

    # [핵심 수정] main.py의 send_message_for_api 호출 시 userId와 sessionId를 전달하도록 변경합니다.
    ai_raw_response = await manager.send_message_for_api(
        request.message, request.healthData, request.userId, request.sessionId
    )
    
    chat_text_for_user = "" 
    notification_payload = None

    try:
        # [핵심 수정] AI 응답을 json으로 먼저 파싱 시도합니다.
        response_data = json.loads(ai_raw_response)
        chat_text_for_user = response_data.get("response_for_user", "오류: AI 응답 형식이 잘못되었습니다.")

        # [핵심 추가] AI가 상태 변경을 요청했는지 확인하고 DB를 업데이트합니다.
        if "status_update" in response_data:
            new_status = response_data["status_update"]
            # manager.db를 통해 firestore 클라이언트에 접근합니다.
            update_user_status(manager.db, request.userId, new_status)
            print(f"🔄 AI 요청에 따라 사용자 상태를 '{new_status}'(으)로 변경했습니다.")
        
        # 기존 알림 생성 로직은 그대로 유지합니다.
        if "[🚨 위험 요소]" in chat_text_for_user:
            if "수면" in chat_text_for_user:
                notification_payload = NotificationPayload(title="수면 부족 경고", body="어젯밤 수면의 질이 좋지 않았습니다. 앱에서 확인해보세요.")
            elif "스트레스" in chat_text_for_user:
                 notification_payload = NotificationPayload(title="높은 스트레스 감지", body="스트레스 지수가 높게 측정되었습니다. 휴식이 필요합니다.")

    except json.JSONDecodeError:
        # JSON 파싱에 실패하면 일반 텍스트 응답으로 간주합니다.
        chat_text_for_user = ai_raw_response.strip()
    except Exception as e:
        print(f"Error processing AI response: {e}")
        chat_text_for_user = "AI 응답을 처리하는 중 오류가 발생했습니다."

    # [핵심 수정] 루틴 설정 후 안내 메시지 추가 로직의 위치를 조정합니다.
    if "캘린더에 등록했" in chat_text_for_user or "일정을 추가했" in chat_text_for_user:
        guidance_message = (
            "\n\n앞으로도 꾸준히 건강 데이터를 모니터링하고, "
            "설정된 루틴 수행 여부에 따라 새로운 알림이나 유용한 정보를 보내드릴게요. 화이팅! 💪"
        )
        chat_text_for_user += guidance_message

    return ChatResponse(
        chatResponse=chat_text_for_user,
        notification=notification_payload
    )