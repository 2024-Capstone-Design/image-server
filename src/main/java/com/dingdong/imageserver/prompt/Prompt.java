package com.dingdong.imageserver.prompt;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Getter
@Table(name = "prompt_table")
public class Prompt {

    @Id
    private String key;

    private String value;

}

