def is_data_sufficient(health_data: dict | None) -> bool:
    """
    전달된 건강 데이터가 AI 분석을 수행하기에 충분한지 확인합니다.
    - healthData 객체가 아예 없는 경우
    - sleep_data나 exercise_data 같은 핵심 키가 누락된 경우
    - 데이터가 비어있는 경우 '불충분'으로 판단합니다.
    """
    if not health_data:
        print("💡 데이터 부족: healthData 객체가 없습니다.")
        return False
    
    # 필수적으로 검사할 핵심 데이터 키 리스트
    required_keys = ["sleep_data", "exercise_data"]
    
    for key in required_keys:
        if key not in health_data or not health_data[key]:
            print(f"💡 데이터 부족: '{key}' 정보가 누락되었거나 비어있습니다.")
            return False
            
    # exercise_data 안의 total_steps가 0인 경우도 데이터 부족으로 간주
    if "exercise_data" in health_data and health_data["exercise_data"]:
        total_steps = health_data["exercise_data"][0].get("stats", {}).get("total_steps", 0)
        if total_steps == 0:
            print("💡 데이터 부족: 총 걸음 수 데이터가 0입니다.")
            return False

    print("✅ 데이터 충분: 분석을 위한 핵심 데이터가 확인되었습니다.")
    return True

def get_health_questionnaire() -> str:
    """
    데이터가 부족할 때 사용자에게 제시할 미니 설문지를 반환합니다.
    """
    return """
    정확한 분석을 위해 몇 가지 추가 정보가 필요해요. 😊
    아래 질문에 간단하게 답변해주시겠어요?
    
    1. 어젯밤 수면의 질은 어떠셨나요? (예: 푹 잤어요, 자주 깼어요)
    2. 오늘 하루 스트레스는 어느 정도였나요? (예: 거의 없었어요, 스트레스가 심했어요)
    3. 오늘 드신 주요 음식은 무엇인가요? (예: 점심에 샐러드, 저녁에 치킨)
    4. 오늘 신체 활동량은 어땠나요? (예: 대부분 앉아 있었어요, 많이 걸었어요)
    5. 현재 가장 개선하고 싶은 건강 목표가 있다면 알려주세요. (예: 체중 감량, 숙면)
    
    답변을 모두 입력해주시면 바로 분석해 드릴게요!
    """