package com.exportgenius.ai.service;

import com.exportgenius.ai.entity.Notification;
import com.exportgenius.ai.entity.User;
import com.exportgenius.ai.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification create(User recipient, String title, String message, String type, String link) {
        Notification notification = Notification.builder()
                .user(recipient)
                .title(title)
                .message(message)
                .type(type)
                .link(link)
                .isRead(false)
                .build();
        return notificationRepository.save(notification);
    }
}
