package com.dailyai.assistant.service;

import com.dailyai.assistant.dto.RoadmapDto;
import com.dailyai.assistant.dto.ProfilePreferencesRequest;
import com.dailyai.assistant.entity.User;
import com.dailyai.assistant.repository.ReadingHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProfilePreferencesTest {
    @Mock CurrentUserService currentUser;
    @Mock ReadingHistoryRepository history;
    @Mock LearningService learning;
    @Mock BookmarkService bookmarks;
    private ProfileService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new ProfileService(currentUser, history, learning, bookmarks);
        user = new User("Test", "test@example.com");
        user.setId(1L);
        when(currentUser.getCurrentUser()).thenReturn(user);
        when(currentUser.getCurrentUserId()).thenReturn(1L);
        when(learning.roadmap()).thenReturn(new RoadmapDto(List.of(), 0, 26, 0));
        when(learning.streakDays()).thenReturn(0);
        when(learning.planProgressPercent()).thenReturn(0);
        when(history.countByUserId(1L)).thenReturn(0L);
        when(history.findTop20ByUserIdOrderByReadAtDesc(1L)).thenReturn(List.of());
        when(bookmarks.count()).thenReturn(0L);
        when(currentUser.updatePreferences(any())).thenAnswer(call -> {
            user.setPreferences(call.getArgument(0));
            return user;
        });
    }

    @Test
    void savesAndReturnsSelectedInterests() {
        var profile = service.updatePreferences(new ProfilePreferencesRequest(List.of("JAVA_BACKEND", "AI_CODING_AGENTS")));

        assertThat(user.getPreferenceList()).containsExactly("JAVA_BACKEND", "AI_CODING_AGENTS");
        assertThat(profile.preferences()).containsExactly("JAVA_BACKEND", "AI_CODING_AGENTS");
    }

    @Test
    void emptySelectionResetsToNeutral() {
        user.setPreferences("JAVA_BACKEND");

        var profile = service.updatePreferences(new ProfilePreferencesRequest(List.of()));

        assertThat(user.getPreferenceList()).isEmpty();
        assertThat(profile.preferences()).isEmpty();
    }

    @Test
    void unknownInterestIsRejectedWithoutSaving() {
        assertThatThrownBy(() -> service.updatePreferences(new ProfilePreferencesRequest(List.of("MY_PRIVATE_HOBBY"))))
                .isInstanceOf(com.dailyai.assistant.exception.BadRequestException.class);
        verify(currentUser, never()).updatePreferences(any());
    }
}
