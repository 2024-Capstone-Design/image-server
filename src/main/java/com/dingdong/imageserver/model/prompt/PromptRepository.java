package com.dingdong.imageserver.model.prompt;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromptRepository extends JpaRepository<Prompt, String> {
    Prompt findByKey(String key);
}
