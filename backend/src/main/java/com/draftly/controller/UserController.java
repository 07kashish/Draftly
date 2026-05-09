package com.draftly.controller;

import com.draftly.dto.DraftResponse;
import com.draftly.dto.UserToneProfileResponse;
import com.draftly.dto.UserResponse;
import com.draftly.service.DraftService;
import com.draftly.service.ToneProfileService;
import com.draftly.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Retrieve users and their AI reply drafts")
public class UserController {

    private final UserService userService;
    private final DraftService draftService;
    private final ToneProfileService toneProfileService;

    @GetMapping("/{email}")
    @Operation(summary = "Get user by email")
    public UserResponse getUser(@PathVariable String email) {
        return userService.toResponse(userService.getByEmail(email));
    }

    @GetMapping("/{email}/drafts")
    @Operation(summary = "Get drafts for user")
    public List<DraftResponse> getDraftsForUser(@PathVariable String email) {
        return draftService.getDraftsForUser(email);
    }

    @GetMapping("/{email}/tone-profile")
    @Operation(summary = "Get learned tone profile for user")
    public UserToneProfileResponse getToneProfile(@PathVariable String email) {
        return toneProfileService.getProfile(email);
    }

    @DeleteMapping("/{email}/tone-profile")
    @Operation(summary = "Reset learned tone profile for user")
    public Map<String, String> resetToneProfile(@PathVariable String email) {
        toneProfileService.resetProfile(email);
        return Map.of("message", "Tone profile reset successfully");
    }
}
