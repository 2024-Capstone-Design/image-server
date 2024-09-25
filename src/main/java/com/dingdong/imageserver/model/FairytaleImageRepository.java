package com.dingdong.imageserver.model;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FairytaleImageRepository extends JpaRepository<FairytaleImage, Long> {
    // fairytaleId로 이미지 목록 조회
    List<FairytaleImage> findByFairytaleId(Long fairytaleId);
}

