package com.example.sharing.message.repository;

import com.example.sharing.message.entity.Message;
import com.example.sharing.message.enums.MessageType;
import com.example.sharing.core.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByReceiverAndType(User receiver, MessageType type, Pageable pageable);

    Page<Message> findByReceiver(User receiver, Pageable pageable);
}