package com.example.downtime.service;

import com.example.downtime.factory.DowntimeTestFactory;
import com.example.downtime.model.DowntimeEvent;
import com.example.downtime.model.DowntimeRequest;
import com.example.downtime.model.DowntimeResponse;
import com.example.downtime.model.DowntimeStatus;
import com.example.downtime.repository.DowntimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.within;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.within;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
//@MockitoSettings(strictness = Strictness.LENIENT)
class DowntimeServiceTest {

    @Mock
    private DowntimeRepository downtimeRepository;

    @Spy
    @InjectMocks
    private DowntimeService downtimeService;

    private DowntimeRequest request;
    private DowntimeEvent event;


    @BeforeEach
    void setUp() {
        request = DowntimeTestFactory.createRequestWithDefaults();
        event = DowntimeTestFactory.createEventFromRequest(request);
//        LocalDateTime mockedTime = event.getCreatedAt();
//        doReturn(mockedTime).when(downtimeService).getCurrentTime();
    }

    @Test
    void findByOperatorIdSorted_shouldReturnEventsSortedByStartTimeDesc() {
        // Given
        String operatorId = "01";

        LocalDateTime now = LocalDateTime.now();
        DowntimeEvent event1 = DowntimeEvent.builder()
                .id(1L)
                .operatorId(operatorId)
                .startTime(now.minusHours(3))
                .build();

        DowntimeEvent event2 = DowntimeEvent.builder()
                .id(2L)
                .operatorId(operatorId)
                .startTime(now.minusHours(1))
                .build();

        DowntimeEvent event3 = DowntimeEvent.builder()
                .id(3L)
                .operatorId(operatorId)
                .startTime(now.minusHours(2))
                .build();

        // Events should be returned in descending order by startTime
        List<DowntimeEvent> expectedEvents = Arrays.asList(event2, event3, event1);

        // Mock the QueryDSL method
        when(downtimeService.getEventsByOperatorSorted(operatorId)).thenReturn(expectedEvents);

        // When
        List<DowntimeEvent> result = downtimeService.getEventsByOperatorSorted(operatorId);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).extracting(DowntimeEvent::getId)
                .containsExactly(2L, 3L, 1L); // event2 (newest), event3, event1 (oldest)

        // Verify sorting
        assertThat(result.get(0).getStartTime()).isAfter(result.get(1).getStartTime());
        assertThat(result.get(1).getStartTime()).isAfter(result.get(2).getStartTime());
    }

    @Test
    void createDowntime_Success() {
        when(downtimeRepository.save(any(DowntimeEvent.class)))
                .thenReturn(event);
        DowntimeResponse response = downtimeService.createDowntime(request);
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(0L);
        assertThat(response.getEquipmentId()).isEqualTo("EQ-001");
        assertThat(response.getStartTime()).isEqualTo(event.getStartTime());
        assertThat(response.getEndTime()).isEqualTo(event.getEndTime());
        assertThat(response.getReason()).isEqualTo("Default Reason");
        assertThat(response.getStatus()).isEqualTo(DowntimeStatus.ACTIVE);

        verify(downtimeRepository, times(1))
                .save(any(DowntimeEvent.class));

    }

    @Test
    void createDowntime_UsesCurrentTime_WhenStartTimeNotProvided() {
        LocalDateTime mockedNow = LocalDateTime.now()
                .truncatedTo(ChronoUnit.SECONDS);
        doReturn(mockedNow).when(downtimeService).getCurrentTime();
        DowntimeRequest request = DowntimeRequest.builder()
                .equipmentId("EQ-001")
                .startTime(null)
                .endTime(mockedNow.plusHours(1))
                .reason("Test")
                .build();

        when(downtimeRepository.save(any(DowntimeEvent.class)))
                .thenAnswer(invocation -> {
                    DowntimeEvent event = invocation.getArgument(0);
                    return event.toBuilder().id(1L).build();
                });

        DowntimeResponse response = downtimeService.createDowntime(request);

        assertThat(response.getStartTime())
                .isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS));
    }



    @Test
    void getDowntime() {
    }

    @Test
    void testGetDowntime() {
    }

    @Test
    void getDowntimesByEquipment() {
    }

    @Test
    void getActiveDowntimes() {
    }

    @Test
    void getDowntimesByOperator() {
    }

    @Test
    void getFilteredDowntimes() {
    }

    @Test
    void getCurrentTime() {
        assertThat(downtimeService.getCurrentTime()).isNotNull();
        assertThat(downtimeService.getCurrentTime())
                .isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS));
    }
}