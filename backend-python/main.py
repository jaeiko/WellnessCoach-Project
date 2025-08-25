# main.py

import uuid
import json
import io
import sys
import os
import asyncio
import google.generativeai as genai
from dotenv import load_dotenv

from firebase_utils import (
    initialize_firebase, save_analysis_json, save_conversation_turn, 
    get_conversation_history, get_user_status, update_user_status
)
from google.genai import types
from google.adk.sessions import InMemorySessionService
from google.adk.runners import Runner
from multi_tool_agent.agent import root_agent

# .env 파일 로드
load_dotenv(dotenv_path="multi_tool_agent/.env")

# 기본 설정
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
genai.configure(api_key=os.getenv("GOOGLE_AI_API_KEY"))

def _load_prompt_for_status(status: str) -> str:
    """사용자 상태에 따라 적절한 프롬프트 파일의 내용을 읽어 반환합니다."""
    prompt_files = {
        "NEEDS_ANALYSIS": "prompts/analytics_prompt.txt",
        "AWAITING_SURVEY_RESPONSE": "prompts/analytics_prompt.txt",
        "ROUTINE_IN_PROGRESS": "prompts/routine_feedback_prompt.txt",
        "GOAL_ACHIEVED": "prompts/new_goal_prompt.txt"
    }
    filepath = prompt_files.get(status, "prompts/analytics_prompt.txt")
    
    print(f"🤖 상태 '{status}'에 따라 '{filepath}' 프롬프트를 로드합니다.")
    try:
        with open(filepath, "r", encoding="utf-8") as f:
            return f.read()
    except FileNotFoundError:
        print(f"🚨 오류: 프롬프트 파일 '{filepath}'을(를) 찾을 수 없습니다.")
        try:
            with open("prompts/analytics_prompt.txt", "r", encoding="utf-8") as f:
                return f.read()
        except FileNotFoundError:
            return "Analyze health data."

class ConversationManager:
    def __init__(self, agent):
        self.agent = agent
        self.db = initialize_firebase()
        ## [수정] 초기화 시 runner와 session_service를 생성하지 않습니다.
        
    async def initialize(self):
        # [수정] 이 함수는 이제 시작 메시지를 출력하는 역할만 합니다.
        print("🤖 Wellness Coach AI가 초기화 준비되었습니다.")

    async def send_message_for_api(self, query: str, health_data: dict | None, user_id: str, session_id: str) -> str:
        """API 요청을 처리하고 AI의 최종 응답 텍스트를 반환하는 전용 함수"""
        
        ## [핵심 수정] 요청마다 세션 서비스와 Runner를 새로 생성합니다.
        print(f"🚀 요청 ID '{session_id}'에 대한 새 Runner를 생성합니다.")
        session_service = InMemorySessionService()
        await session_service.create_session(app_name="wellness_coach_app", user_id=user_id, session_id=session_id)
        
        runner = Runner(
            agent=self.agent,
            app_name="wellness_coach_app",
            session_service=session_service
        )
        
        # 1. 사용자의 현재 대화 상태 조회
        user_status = get_user_status(self.db, user_id)
        
        # 2. 상태에 맞는 프롬프트 동적 로드
        prompt_template = _load_prompt_for_status(user_status)
        
        # 3. 프롬프트에 데이터 주입 (기존 로직과 동일)
        final_prompt = prompt_template
        if health_data:
            final_prompt = final_prompt.replace("((USER_PROFILE))", json.dumps(health_data.get("user_profile", {}), ensure_ascii=False))
            final_prompt = final_prompt.replace("((CURRENT_HEALTH_DATA))", json.dumps(health_data, ensure_ascii=False))
            # ... (다른 데이터 placeholder들도 필요 시 추가) ...
        
        history_list = get_conversation_history(db=self.db, user_id=user_id)
        history_str = "\n".join(history_list)
        final_prompt = final_prompt.replace("((CONVERSATION_HISTORY))", history_str)
        final_prompt = final_prompt.replace("((USER_GOAL))", query)
        
        full_query = f"{final_prompt}\n\nLatest User Query: {query}"
        content = types.Content(role='user', parts=[types.Part(text=full_query)])
        
        final_response_text = "죄송합니다, 답변을 생성하는 데 실패했습니다."
        # [수정] 새로 만든 runner를 사용합니다.
        async for event in runner.run_async(user_id=user_id, session_id=session_id, new_message=content):
            if event.is_final_response():
                final_response_text = event.content.parts[0].text
                break
        
        save_conversation_turn(
            db=self.db, user_id=user_id,
            session_id=session_id,
            user_query=query, ai_response=final_response_text
        )
        
        if user_status == 'NEEDS_ANALYSIS' and "analysis_json" in final_response_text:
             update_user_status(self.db, user_id, "ROUTINE_IN_PROGRESS")

        return final_response_text