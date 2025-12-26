package com.example.downtime.listener;

import com.example.downtime.model.DowntimeEvent;
import com.example.downtime.service.SequenceGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DowntimeEventModelListener extends AbstractMongoEventListener<DowntimeEvent> {

    private final SequenceGeneratorService sequenceGenerator;

    @Override
    public void onBeforeConvert(BeforeConvertEvent<DowntimeEvent> event) {
        if (event.getSource().getId() == null) {
            event.getSource().setId(
                    sequenceGenerator.generateSequence(DowntimeEvent.SEQUENCE_NAME)
            );
        }
    }
}
