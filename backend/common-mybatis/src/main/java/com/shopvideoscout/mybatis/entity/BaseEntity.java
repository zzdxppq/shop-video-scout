package com.shopvideoscout.mybatis.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Base entity with common fields.
 * All entities should extend this class.
 */
@Data
public abstract class BaseEntity implements Serializable {

    /**
     * Primary key ID (auto-increment).
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Creation timestamp (auto-filled on insert).
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * Update timestamp (auto-filled on insert and update).
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
