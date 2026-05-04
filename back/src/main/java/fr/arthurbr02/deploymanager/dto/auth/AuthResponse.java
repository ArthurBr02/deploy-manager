package fr.arthurbr02.deploymanager.dto.auth;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@JsonSubTypes({
    @JsonSubTypes.Type(value = LoginResponse.class),
    @JsonSubTypes.Type(value = MfaRequiredResponse.class)
})
public sealed interface AuthResponse permits LoginResponse, MfaRequiredResponse {}
