package com.shopvideoscout.task.dto;

import lombok.Data;

/**
 * Request DTO for triggering compose.
 * Voice type is already stored on the task; this DTO is intentionally minimal.
 */
@Data
public class ComposeRequest {
    // Future: additional compose parameters can be added here
    // Currently, voice_type and voice_sample_id are read from the task entity
}
