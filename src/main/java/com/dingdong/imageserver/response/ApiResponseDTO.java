package com.dingdong.imageserver.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.http.ResponseEntity;

@Builder
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponseDTO<T> {

    @Schema(description = "HTTP 상태 코드", example = "200")
    private final int status;

    @Schema(description = "성공 여부", example = "true")
    private final boolean success;

    @Schema(description = "응답 메시지", example = "요청이 성공적으로 처리되었습니다.")
    private final String message;

    @Schema(description = "응답 데이터")
    private T data;

    public static <T> ResponseEntity<ApiResponseDTO<T>> success(SuccessStatus successStatus) {
        return ResponseEntity.status(successStatus.getHttpStatus())
                .body(ApiResponseDTO.<T>builder()
                        .status(successStatus.getStatusCode())
                        .success(true)
                        .message(successStatus.getMessage()).build());
    }

    public static <T> ResponseEntity<ApiResponseDTO<T>> success(SuccessStatus successStatus, T data) {
        return ResponseEntity.status(successStatus.getHttpStatus())
                .body(ApiResponseDTO.<T>builder()
                        .status(successStatus.getStatusCode())
                        .success(true)
                        .message(successStatus.getMessage())
                        .data(data).build());
    }

    public static <T> ResponseEntity<ApiResponseDTO<T>> fail(ErrorStatus errorStatus) {
        return ApiResponseDTO.fail(errorStatus.getHttpStatus().value(),  errorStatus.getDetail());
    }


    public static <T> ResponseEntity<ApiResponseDTO<T>> fail(ErrorStatus errorStatus, String message) {
        String fullErrorMessage = message != null ? errorStatus.getDetail() + " " + message : errorStatus.getDetail();
        return ApiResponseDTO.fail(errorStatus.getHttpStatus().value(), fullErrorMessage);
    }

    public static <T> ResponseEntity<ApiResponseDTO<T>> fail(int status, String message) {
        return ResponseEntity.status(status)
                .body(ApiResponseDTO.<T>builder()
                        .status(status)
                        .success(false)
                        .message(message).build());
    }

}