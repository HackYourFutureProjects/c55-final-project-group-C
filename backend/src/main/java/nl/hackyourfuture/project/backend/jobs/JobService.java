package nl.hackyourfuture.project.backend.jobs;

import nl.hackyourfuture.project.backend.jobs.dto.JobDetailResponse;
import nl.hackyourfuture.project.backend.jobs.dto.JobFiltersResponse;
import nl.hackyourfuture.project.backend.jobs.dto.JobSearchResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class JobService {

    private final JobRepository jobRepository;

    public JobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    // Coordinate job search filtering requests through the repository
    public List<JobSearchResponse> searchJobs(String discipline, String workMode, String location, String q) {
        return jobRepository.searchJobs(discipline, workMode, location, q);
    }

    // Retrieve available filter options for frontend dropdowns
        public JobFiltersResponse getAvailableFilters() {
            return jobRepository.getAvailableFilters();
        }

    // Retrieve a specific job posting or throws a 404 error if not found
    public JobDetailResponse getJobById(String postingId) {
        return jobRepository.getJobById(postingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job posting not found"));
    }
}