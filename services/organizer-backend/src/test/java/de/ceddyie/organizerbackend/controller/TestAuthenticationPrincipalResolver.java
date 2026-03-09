package de.ceddyie.organizerbackend.controller;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Test helper that resolves @AuthenticationPrincipal Long parameters
 * to a fixed userId value in standalone MockMvc tests.
 */
class TestAuthenticationPrincipalResolver implements HandlerMethodArgumentResolver {

    private final Long userId;

    TestAuthenticationPrincipalResolver(Long userId) {
        this.userId = userId;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                && Long.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        return userId;
    }
}
