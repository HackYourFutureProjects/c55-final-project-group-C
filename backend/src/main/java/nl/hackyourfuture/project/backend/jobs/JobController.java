package nl.hackyourfuture.project.backend.jobs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import nl.hackyourfuture.project.backend.jobs.dto.JobDetailResponse;
import nl.hackyourfuture.project.backend.jobs.dto.JobFiltersResponse;
import nl.hackyourfuture.project.backend.jobs.dto.JobSearchResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@Tag(name = "Jobs", description = "Endpoints for searching and viewing job postings")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    // search and filter job postings
    @GetMapping
    @Operation(summary = "Search job postings with optional filters")
    @ApiResponse(responseCode = "200", description = "Jobs retrieved successfully")
    public ResponseEntity<List<JobSearchResponse>> searchJobs(
            @RequestParam(required = false) String discipline,
            @RequestParam(required = false) String workMode,
            @RequestParam(required = false) String location
    ) {
        List<JobSearchResponse> jobs = jobService.searchJobs(discipline, workMode, location);
        return ResponseEntity.ok(jobs);
    }

    // retrieve available filter options for search dropdowns
    @GetMapping("/filters")
        @Operation(summary = "Get available filter options for search dropdowns")
        @ApiResponse(responseCode = "200", description = "Filters retrieved successfully")
        public ResponseEntity<JobFiltersResponse> getJobFilters() {
            JobFiltersResponse filters = jobService.getAvailableFilters();
            return ResponseEntity.ok(filters);
        }

    // fetch complete details of a single job posting by ID
    @GetMapping("/{postingId}")
    @Operation(summary = "Get full details of a single job posting by ID")
    @ApiResponse(responseCode = "200", description = "Job details retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Job posting not found")
    public ResponseEntity<JobDetailResponse> getJobById(
            @PathVariable String postingId
    ) {
        JobDetailResponse job = jobService.getJobById(postingId);
        return ResponseEntity.ok(job);
    }
}