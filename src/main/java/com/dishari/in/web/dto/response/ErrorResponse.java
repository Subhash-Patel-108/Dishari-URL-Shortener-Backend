package com.dishari.in.web.dto.response;


import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

public class ErrorResponse {
    private boolean success ;
    private String timestamp ;
    private int status ;
    private String message ;
    private String error ;
    private String path ;
    private List<String> errors ;

}
