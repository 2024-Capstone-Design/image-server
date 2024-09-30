package com.dingdong.imageserver.model.task;

import com.dingdong.imageserver.enums.TaskAction;
import com.dingdong.imageserver.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.*;


@Table(name = "mj_task")
@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Task {

    @Id
    private Long serialVersionUID;

    private TaskAction action;
    private TaskStatus status;
    private String prompt;
    private String promptEn;
    private String description;
    private String state;
    private Long submitTime;
    private Long startTime;
    private Long finishTime;
    private String imageUrl;
    private String progress;
    private String failReason;

    public boolean isSuccessful() {
        return TaskStatus.SUCCESS.equals(this.status);
    }

    public boolean isFailure() {
        return TaskStatus.FAILURE.equals(this.status);
    }
}
