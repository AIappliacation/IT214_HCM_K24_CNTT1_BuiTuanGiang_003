package org.example.enrollmentservice.models.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.enrollmentservice.models.constants.EnrollmentStatus;
import org.example.enrollmentservice.exceptions.DuplicateCourseException;
import org.example.enrollmentservice.models.dto.requests.CreateEnrollmentDetailRequest;
import org.example.enrollmentservice.models.dto.requests.CreateEnrollmentRequest;
import org.example.enrollmentservice.models.dto.responses.EnrollmentDetailResponse;
import org.example.enrollmentservice.models.dto.responses.EnrollmentResponse;
import org.example.enrollmentservice.models.dto.responses.CourseResponse;
import org.example.enrollmentservice.models.entities.Enrollment;
import org.example.enrollmentservice.models.entities.EnrollmentDetail;
import org.example.enrollmentservice.models.repositories.EnrollmentDetailRepository;
import org.example.enrollmentservice.models.repositories.EnrollmentRepository;
import org.example.enrollmentservice.models.services.EnrollmentService;
import org.example.enrollmentservice.services.KafkaProducerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {

        private final EnrollmentRepository enrollmentRepository;
        private final EnrollmentDetailRepository enrollmentDetailRepository;
        private final CourseGatewayService courseGatewayService;
        private final KafkaProducerService kafkaProducerService;

        @Override
        @Transactional
        public EnrollmentResponse createEnrollment(CreateEnrollmentRequest request) {
                Set<Long> courseIds = new HashSet<>();
                for (CreateEnrollmentDetailRequest item : request.items()) {
                        if (!courseIds.add(item.courseId())) {
                                throw new DuplicateCourseException(item.courseId());
                        }
                }

                Enrollment enrollment = Enrollment.builder()
                        .studentName(request.studentName())
                        .studentEmail(request.studentEmail())
                        .status(EnrollmentStatus.PENDING)
                        .totalFee(0.0)
                        .build();

                enrollment = enrollmentRepository.save(enrollment);

                List<EnrollmentDetailResponse> detailResponses = new ArrayList<>();
                double totalFee = 0.0;

                for (CreateEnrollmentDetailRequest item : request.items()) {
                        CourseResponse course = courseGatewayService.getCourseById(item.courseId());

                        double courseFee = course.courseFee();
                        totalFee += courseFee;

                        EnrollmentDetail detail = EnrollmentDetail.builder()
                                .enrollment(enrollment)
                                .courseId(item.courseId())
                                .courseFee(courseFee)
                                .build();

                        detail = enrollmentDetailRepository.save(detail);

                        EnrollmentDetailResponse detailResponse = new EnrollmentDetailResponse(
                                detail.getId(),
                                detail.getCourseId(),
                                course.courseName(),
                                detail.getCourseFee(),
                                courseFee
                        );

                        detailResponses.add(detailResponse);
                }

                enrollment.setTotalFee(totalFee);
                enrollment = enrollmentRepository.save(enrollment);

                kafkaProducerService.sendEnrollmentCreatedEvent(request.studentEmail());

                return new EnrollmentResponse(
                        enrollment.getId(),
                        enrollment.getStudentName(),
                        enrollment.getStudentEmail(),
                        enrollment.getTotalFee(),
                        enrollment.getStatus(),
                        detailResponses
                );
        }

}
