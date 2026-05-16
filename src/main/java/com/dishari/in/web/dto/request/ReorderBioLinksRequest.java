package com.dishari.in.web.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record ReorderBioLinksRequest(

        @NotNull(message = "Ordered link IDs are required")
        @Size(min = 1, message = "At least one link ID required")
        List<UUID> orderedLinkIds
) {}