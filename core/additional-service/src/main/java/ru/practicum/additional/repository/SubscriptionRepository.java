package ru.practicum.additional.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.additional.model.Subscription;
import ru.practicum.additional.model.SubscriptionStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    boolean existsBySubscriberIdAndPublisherId(Long subscriberId, Long publisherId);

    Optional<Subscription> findBySubscriberIdAndPublisherId(Long subscriberId, Long publisherId);

    Page<Subscription> findAllByPublisherId(Long publisherId, Pageable pageable);

    Page<Subscription> findAllBySubscriberId(Long subscriberId, Pageable pageable);

    @Query("SELECT s FROM Subscription s WHERE s.subscriberId = :userId AND s.status = :status")
    List<Subscription> findAllBySubscriberIdAndStatus(@Param("userId") Long userId,
                                                      @Param("status") SubscriptionStatus status);

    @Query("SELECT s FROM Subscription s WHERE s.publisherId = :userId AND s.status = :status")
    List<Subscription> findAllByPublisherIdAndStatus(@Param("userId") Long userId,
                                                     @Param("status") SubscriptionStatus status);
}