package de.ceddyie.organizerbackend.repository;

import de.ceddyie.organizerbackend.enums.AttendanceStatus;
import de.ceddyie.organizerbackend.model.EventAttendee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendanceRepository extends JpaRepository<EventAttendee, Long> {
    int countAllByEventId(Long id);

    int countByEventIdAndStatus(Long id, AttendanceStatus attendanceStatus);
}
