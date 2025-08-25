# firebase_utils.py

import firebase_admin
from firebase_admin import credentials, firestore
from datetime import datetime
import json
from google.cloud.firestore_v1.base_query import FieldFilter


def initialize_firebase():
    """
    Firebase 앱을 초기화하고 Firestore 클라이언트를 반환합니다.
    이미 초기화된 경우, 기존 앱을 사용합니다.
    """
    try:
        # 앱이 이미 초기화되었는지 확인
        firebase_admin.get_app()
    except ValueError:
        # 초기화되지 않았다면, 서비스 계정 키를 사용하여 초기화
        cred = credentials.Certificate("firebase_credentials.json")
        firebase_admin.initialize_app(cred)

    return firestore.client()


def save_analysis_json(db, user_id: str, session_id: str, analysis_data: dict):
    """
    구조화된 JSON 분석 결과를 Firestore에 저장합니다.
    """
    # 문서 ID를 타임스탬프로 하여 시간순 정렬이 용이하게 함
    doc_id = datetime.now().strftime("%Y-%m-%d_%H:%M:%S")
    doc_ref = db.collection('users').document(
        user_id).collection('analysis_history').document(doc_id)

    analysis_data['timestamp'] = firestore.SERVER_TIMESTAMP
    doc_ref.set(analysis_data)
    print(f"✅ Firestore에 분석 결과 저장 완료: {user_id}/{doc_id}")


def get_user_status(db, user_id: str) -> str:
    """
    Firestore에서 사용자의 현재 대화 상태를 가져옵니다.
    상태가 없으면 'NEEDS_ANALYSIS'를 기본값으로 반환합니다.
    """
    doc_ref = db.collection('users').document(user_id)
    doc = doc_ref.get()
    if doc.exists:
        # to_dict()의 get 메서드를 사용하여 'status' 키가 없을 경우 기본값 반환
        return doc.to_dict().get('status', 'NEEDS_ANALYSIS')
    else:
        # 사용자가 아예 존재하지 않는 경우
        return 'NEEDS_ANALYSIS'
    

def update_user_status(db, user_id: str, new_status: str):
    """
    Firestore에서 사용자의 대화 상태를 업데이트(변경)합니다.
    """
    doc_ref = db.collection('users').document(user_id)
    # set 메서드에 merge=True를 사용하여 다른 필드는 유지하고 status 필드만 추가/수정
    doc_ref.set({'status': new_status}, merge=True)
    print(f"✅ 사용자 '{user_id}'의 상태를 '{new_status}'(으)로 업데이트했습니다.")


def get_user_profile(db, user_id: str) -> dict | None:
    """
    Firestore에서 사용자 프로필 정보를 가져옵니다. 없으면 None을 반환합니다.
    """
    doc_ref = db.collection('users').document(user_id)
    doc = doc_ref.get()
    if doc.exists and doc.to_dict().get('profile'):
        return doc.to_dict()['profile']
    else:
        # 프로필이 없으면 None을 반환
        return None


def save_conversation_turn(db, user_id: str, session_id: str, user_query: str, ai_response: str):
    """
    한 턴의 대화(사용자 질문 + AI 답변)를 Firestore에 저장합니다.
    """
    timestamp_doc_id = datetime.now().strftime("%Y-%m-%d_%H:%M:%S.%f")
    doc_ref = db.collection('users').document(user_id).collection('sessions').document(
        session_id).collection('conversation_history').document(timestamp_doc_id)
    turn_data = {
        'user_id': user_id,
        'user_query': user_query,
        'ai_response': ai_response,
        'timestamp': firestore.SERVER_TIMESTAMP
    }
    doc_ref.set(turn_data)
    print(f"💬 Firestore에 대화 저장 완료: {user_id}/{session_id}/{timestamp_doc_id}")


def get_conversation_history(db, user_id: str, limit: int = 10) -> list:
    """
    Firestore에서 특정 사용자의 모든 세션을 통틀어 최근 대화 기록을 가져옵니다.
    """
    history_ref = db.collection_group('conversation_history').where(
        filter=FieldFilter('user_id', '==', user_id)
    ).order_by(
        'timestamp', direction=firestore.Query.DESCENDING
    ).limit(limit)

    docs = history_ref.stream()
    history = []
    for doc in docs:
        data = doc.to_dict()
        ai_text = data.get('ai_response', '')
        try:
            ai_data = json.loads(ai_text)
            ai_text = ai_data.get('response_for_user', ai_text)
        except json.JSONDecodeError:
            pass

        history.append(f"User: {data.get('user_query')}")
        history.append(f"AI: {ai_text}")

    return list(reversed(history))
