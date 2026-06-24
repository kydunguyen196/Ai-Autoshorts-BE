package com.autoshorts.ai.repository;

import com.autoshorts.ai.entity.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppSettingRepository extends JpaRepository<AppSetting, String> {

    List<AppSetting> findAllByCategoryOrderByKeyAsc(String category);
}
