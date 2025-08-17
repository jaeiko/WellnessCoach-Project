# main.py

# 1. .env 파일을 가장 먼저 로드
import uuid
import json
from firebase_utils import initialize_firebase, save_analysis_json, save_conversation_turn, get_conversation_history
import io
import sys
from google.genai import types
from google.adk.sessions import InMemorySessionService
from google.adk.runners import Runner
import asyncio
import os
import google.generativeai as genai
from multi_tool_agent.tools import get_health_data
from multi_tool_agent.agent import root_agent
from dotenv import load_dotenv
load_dotenv(dotenv_path="multi_tool_agent/.env")

# 2. 나머지 모듈 import

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
genai.configure(api_key=os.getenv("GOOGLE_AI_API_KEY"))


class ConversationManager:
    def __init__(self, agent):
        self.agent = agent
        self.runner = None
        self.db = initialize_firebase()
        self.session_info = {
            "app_name": "wellness_coach_app",
            "user_id": "user_1",
            "session_id": f"session_{uuid.uuid4()}"
        }

    async def initialize(self):
        session_service = InMemorySessionService()
        await session_service.create_session(**self.session_info)
        self.runner = Runner(
            agent=self.agent,
            app_name=self.session_info["app_name"],
            session_service=session_service
        )
        print("🤖 Wellness Coach AI가 초기화되었습니다.")

    async def send_message(self, query):
        print(f"\n> 당신: {query}")

        full_query = ""
        prompt_template = ""

        try:
            with open("prompts/analytics_prompt.txt", "r", encoding="utf-8") as f:
                prompt_template = f.read()
        except FileNotFoundError:
            print("\nAI 비서: 오류: analytics_prompt.txt 파일을 찾을 수 없습니다.")
            return

        # [지휘자 로직] "분석" 키워드가 있을 때만 Python이 직접 데이터를 가져옵니다.
        if "분석" in query or "진단" in query:
            print("\nSYSTEM: Analysis request detected. Fetching health data...")
            health_data_str = get_health_data()
            health_data = json.loads(health_data_str)

            if "error" in health_data:
                print(f"\nAI 비서: {health_data['error']}")
                return

            # 프롬프트 템플릿에 데이터를 주입합니다.
            final_prompt = prompt_template.replace("((USER_GOAL))", query)
            final_prompt = final_prompt.replace("((USER_PROFILE))", json.dumps(
                health_data.get("user_profile", {}), ensure_ascii=False))
            final_prompt = final_prompt.replace("((TIMESERIES_DATA))", json.dumps(
                health_data.get("timeseries_data", []), ensure_ascii=False))
            final_prompt = final_prompt.replace("((SLEEP_DATA))", json.dumps(
                health_data.get("sleep_data", {}), ensure_ascii=False))
            final_prompt = final_prompt.replace("((EXERCISE_DATA))", json.dumps(
                health_data.get("exercise_data", []), ensure_ascii=False))
            final_prompt = final_prompt.replace("((NUTRITION_DATA))", json.dumps(
                health_data.get("nutrition_data", {}), ensure_ascii=False))
            final_prompt = final_prompt.replace("((VITALS_DATA))", json.dumps(
                health_data.get("vitals_data", {}), ensure_ascii=False))

            # 분석 턴에도 대화 기록은 전달하여 AI가 재분석 요청인지 등을 판단하게 합니다.
            history_list = get_conversation_history(
                db=self.db, user_id=self.session_info["user_id"])
            history_str = "\n".join(history_list)
            final_prompt = final_prompt.replace(
                "((CONVERSATION_HISTORY))", history_str)

            full_query = final_prompt
        else:
            # "분석"이 아닌 일반 대화(코칭)는 프롬프트와 이전 대화 기록, 그리고 새 질문만 전달합니다.
            history_list = get_conversation_history(
                db=self.db,
                user_id=self.session_info["user_id"]
            )  # 👈 [버그 수정 완료] 문제가 되었던 잘못된 인자 전달을 수정한 부분입니다.
            history_str = "\n".join(history_list)
            final_prompt = prompt_template.replace(
                "((CONVERSATION_HISTORY))", history_str)
            full_query = f"{final_prompt}\n\nLatest User Query: {query}"

        content = types.Content(
            role='user', parts=[types.Part(text=full_query)])

        final_response_text = None
        async for event in self.runner.run_async(user_id=self.session_info["user_id"], session_id=self.session_info["session_id"], new_message=content):
            if event.is_final_response():
                final_response_text = event.content.parts[0].text
                break

        if final_response_text:
            clean_text = final_response_text.strip()

            save_conversation_turn(
                db=self.db, user_id=self.session_info["user_id"],
                session_id=self.session_info["session_id"],
                user_query=query, ai_response=clean_text
            )

            try:
                response_data = json.loads(clean_text)
                analysis_json = response_data.get("analysis_json", {})
                response_for_user = response_data.get(
                    "response_for_user", "오류: 분석 결과를 해석할 수 없습니다.")
                print(f"\nAI 비서: {response_for_user}")
                if analysis_json:
                    save_analysis_json(
                        db=self.db, user_id=self.session_info["user_id"],
                        session_id=self.session_info["session_id"],
                        analysis_data=analysis_json
                    )
            except json.JSONDecodeError:
                print(f"\nAI 비서: {clean_text}")
        else:
            print("\nAI 비서: 죄송합니다, 답변을 생성하는 데 실패했습니다.")


async def main():
    manager = ConversationManager(agent=root_agent)
    await manager.initialize()
    await manager.send_message(input("AI에게 먼저 분석을 시작하도록 지시하세요: "))
    while True:
        user_input = input("\n> ")
        if user_input.lower() in ["종료", "끝", "exit"]:
            break
        await manager.send_message(user_input)
    print("\n대화를 종료합니다.")

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\n사용자에 의해 프로그램이 종료되었습니다.")
