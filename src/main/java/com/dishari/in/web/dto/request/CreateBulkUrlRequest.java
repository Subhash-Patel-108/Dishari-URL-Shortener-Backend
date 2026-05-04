package com.dishari.in.web.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateBulkUrlRequest (
        @NotNull(message = "Url cannot be null")
        @Size(max = 100, message = "Maximum 100 url allowed per request")
        List<CreateNormalUrlRequest> urls
){}
