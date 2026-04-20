package com.cy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cy.entity.Session;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface SessionMapper extends BaseMapper<Session> {
	/**
	 * 分页列表（配合 PageHelper 使用）
	 */
	IPage<Session> pageList(Page<Session> page,
							@Param("userId") Long userId,
							@Param("begin") LocalDateTime begin,
							@Param("end") LocalDateTime end);


	/**
	 * 按用户+日期聚合营养数据（SQL层计算）
	 */
	@Select("""
        SELECT
            IFNULL(SUM(total_calories), 0) AS totalCalories,
            IFNULL(SUM(protein), 0) AS protein,
            IFNULL(SUM(fat), 0) AS fat,
            IFNULL(SUM(carbs), 0) AS carbs
        FROM session
        WHERE user_id = #{userId}
          AND create_time BETWEEN #{start} AND #{end}
    """)
	Map<String, Object> sumByUserAndDate(
			@Param("userId") Long userId,
			@Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end
	);
}
