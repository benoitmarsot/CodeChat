package com.unbumpkin.codechat.dto.social;

import com.unbumpkin.codechat.service.social.SocialService.SocialPlatforms;

public record AddSocialRequest(int projectId, String workspaceId, String pat, SocialPlatforms platform) {

}
