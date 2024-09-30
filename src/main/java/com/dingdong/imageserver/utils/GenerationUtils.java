package com.dingdong.imageserver.utils;

import com.dingdong.imageserver.dto.firebase.BackgroundDTO;
import com.dingdong.imageserver.dto.firebase.CharacterDTO;
import com.dingdong.imageserver.dto.service.CommonImageGenerationDTO;
import com.dingdong.imageserver.enums.PromptType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class GenerationUtils {

    /**
     * 주인공을 찾는 메소드
     * @param characters 캐릭터 목록
     * @return 주인공 캐릭터 프롬프트
     */
    public CommonImageGenerationDTO findProtagonist(List<CharacterDTO> characters) {
        return characters.stream()
                .filter(CharacterDTO::isMain)
                .findFirst()
                .map(c -> new CommonImageGenerationDTO(PromptType.CHARACTER, c.getName(), c.getPrompt()))
                .orElse(null);
    }

    /**
     * 비주인공 캐릭터를 찾는 메소드
     * @param characters 캐릭터 목록
     * @return 비주인공 캐릭터 프롬프트 목록
     */
    public List<CommonImageGenerationDTO> findNonProtagonists(List<CharacterDTO> characters) {
        return characters.stream()
                .filter(c -> !c.isMain())
                .map(c -> new CommonImageGenerationDTO(PromptType.CHARACTER, c.getName(), c.getPrompt()))
                .collect(Collectors.toList());
    }

    /**
     * 배경 프롬프트를 찾는 메소드
     * @param backgrounds 배경 목록
     * @return 배경 프롬프트 목록
     */
    public List<CommonImageGenerationDTO> findBackgrounds(List<BackgroundDTO> backgrounds) {
        return backgrounds.stream()
                .map(b -> new CommonImageGenerationDTO(PromptType.BACKGROUND, b.getName(), b.getPrompt()))
                .collect(Collectors.toList());
    }
}
