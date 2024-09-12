package com.dingdong.imageserver.enums;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class ReturnCode {
	/**
	 * 성공.
	 */
	public static final int SUCCESS = 1;
	/**
	 * 데이터를 찾을 수 없음.
	 */
	public static final int NOT_FOUND = 3;
	/**
	 * 검증 오류.
	 */
	public static final int VALIDATION_ERROR = 4;
	/**
	 * 시스템 예외.
	 */
	public static final int FAILURE = 9;

	/**
	 * 이미 존재함.
	 */
	public static final int EXISTED = 21;
	/**
	 * 대기 중.
	 */
	public static final int IN_QUEUE = 22;
	/**
	 * 대기열이 가득 참.
	 */
	public static final int QUEUE_REJECTED = 23;
	/**
	 * prompt에 민감한 단어가 포함됨.
	 */
	public static final int BANNED_PROMPT = 24;
}
