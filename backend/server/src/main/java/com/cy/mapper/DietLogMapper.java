package com.cy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cy.entity.DietLog;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;

@Mapper
public interface DietLogMapper extends BaseMapper<DietLog> {
    /**
     * 插入或更新（MySQL UPSERT）
     */
    @Insert("""
        INSERT INTO diet_log
        (user_id, date, total_calories, protein, fat, carbs)
        VALUES
        (#{userId}, #{date}, #{totalCalories}, #{protein}, #{fat}, #{carbs})
        ON DUPLICATE KEY UPDATE
            total_calories = VALUES(total_calories),
            protein = VALUES(protein),
            fat = VALUES(fat),
            carbs = VALUES(carbs)
    """)
    void upsert(
            @Param("userId") Long userId,
            @Param("date") LocalDate date,
            @Param("totalCalories") Double totalCalories,
            @Param("protein") Double protein,
            @Param("fat") Double fat,
            @Param("carbs") Double carbs
    );

    /**
     * 查询某天记录（给 advice 用）
     */
    @Select("""
        SELECT id,user_id,date,total_calories,protein,fat,carbs,create_time
        WHERE user_id = #{userId}
          AND date = #{date}
        LIMIT 1
    """)
    DietLog selectByUserAndDate(
            @Param("userId") Long userId,
            @Param("date") LocalDate date
    );
}

