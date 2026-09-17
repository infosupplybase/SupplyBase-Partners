package com.supplybase.partners.catalog;

import com.supplybase.partners.catalog.dto.*;
import com.supplybase.partners.partner.WorkingHoursChoice;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {

    private final CategoryRepository categoryRepository;
    private final ServiceRepository serviceRepository;
    private final CityRepository cityRepository;
    private final AreaRepository areaRepository;
    private final CategoryCityEstimateRepository estimateRepository;
    private final PolicyDocumentRepository policyDocumentRepository;

    public CatalogController(CategoryRepository categoryRepository, ServiceRepository serviceRepository,
                              CityRepository cityRepository, AreaRepository areaRepository,
                              CategoryCityEstimateRepository estimateRepository,
                              PolicyDocumentRepository policyDocumentRepository) {
        this.categoryRepository = categoryRepository;
        this.serviceRepository = serviceRepository;
        this.cityRepository = cityRepository;
        this.areaRepository = areaRepository;
        this.estimateRepository = estimateRepository;
        this.policyDocumentRepository = policyDocumentRepository;
    }

    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return categoryRepository.findAllByActiveTrueOrderByDisplayOrder().stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getIconUrl(), c.getImageUrl()))
                .toList();
    }

    @GetMapping("/categories/{categoryId}/services")
    public List<ServiceResponse> servicesForCategory(@PathVariable Long categoryId) {
        return serviceRepository.findAllByCategoryIdAndActiveTrueOrderByDisplayOrder(categoryId).stream()
                .map(s -> new ServiceResponse(s.getId(), s.getCategoryId(), s.getName(), s.getSlug()))
                .toList();
    }

    @GetMapping("/cities")
    public List<CityResponse> cities() {
        return cityRepository.findAllByActiveTrueOrderByName().stream()
                .map(c -> new CityResponse(c.getId(), c.getName(), c.getState()))
                .toList();
    }

    @GetMapping("/cities/{cityId}/areas")
    public List<AreaResponse> areasForCity(@PathVariable Long cityId) {
        return areaRepository.findAllByCityIdAndActiveTrueOrderByName(cityId).stream()
                .map(a -> new AreaResponse(a.getId(), a.getCityId(), a.getName()))
                .toList();
    }

    @GetMapping("/estimates")
    public EstimateResponse estimate(@RequestParam Long categoryId, @RequestParam Long cityId,
                                      @RequestParam WorkingHoursChoice hours) {
        return estimateRepository.findByCategoryIdAndCityIdAndHoursChoiceAndActiveTrue(categoryId, cityId, hours)
                .map(e -> new EstimateResponse(true, true, e.getEstimatedMonthlyPaise(), e.getAssumptionsText()))
                .orElseGet(EstimateResponse::unavailable);
    }

    @GetMapping("/policies/current")
    public List<PolicyDocumentResponse> currentPolicies() {
        return policyDocumentRepository.findAllByCurrentTrue().stream()
                .map(p -> new PolicyDocumentResponse(p.getId(), p.getType().name(), p.getVersion(), p.getTitle(), p.getContentMarkdown()))
                .toList();
    }
}
