package com.supplybase.partners.catalog;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/catalog")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCatalogController {

    private final CategoryRepository categoryRepository;
    private final ServiceRepository serviceRepository;
    private final CityRepository cityRepository;
    private final AreaRepository areaRepository;
    private final CategoryCityEstimateRepository estimateRepository;
    private final PolicyDocumentRepository policyDocumentRepository;

    public AdminCatalogController(CategoryRepository categoryRepository, ServiceRepository serviceRepository,
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

    @PostMapping("/categories")
    public Category createCategory(@RequestBody Map<String, Object> body) {
        Category c = new Category();
        c.setName(body.get("name").toString());
        c.setSlug(body.get("slug").toString());
        c.setIconUrl((String) body.get("iconUrl"));
        c.setImageUrl((String) body.get("imageUrl"));
        c.setDisplayOrder(body.containsKey("displayOrder") ? Integer.parseInt(body.get("displayOrder").toString()) : 0);
        return categoryRepository.save(c);
    }

    @PatchMapping("/categories/{id}")
    public Category updateCategory(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Category c = categoryRepository.findById(id).orElseThrow();
        if (body.containsKey("name")) c.setName(body.get("name").toString());
        if (body.containsKey("active")) c.setActive(Boolean.parseBoolean(body.get("active").toString()));
        if (body.containsKey("displayOrder")) c.setDisplayOrder(Integer.parseInt(body.get("displayOrder").toString()));
        if (body.containsKey("iconUrl")) c.setIconUrl((String) body.get("iconUrl"));
        if (body.containsKey("imageUrl")) c.setImageUrl((String) body.get("imageUrl"));
        return categoryRepository.save(c);
    }

    @PostMapping("/services")
    public Service createService(@RequestBody Map<String, Object> body) {
        Service s = new Service();
        s.setCategoryId(Long.valueOf(body.get("categoryId").toString()));
        s.setName(body.get("name").toString());
        s.setSlug(body.get("slug").toString());
        return serviceRepository.save(s);
    }

    @PostMapping("/cities")
    public City createCity(@RequestBody Map<String, Object> body) {
        City c = new City();
        c.setName(body.get("name").toString());
        c.setState(body.get("state").toString());
        return cityRepository.save(c);
    }

    @PatchMapping("/cities/{id}")
    public City updateCity(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        City c = cityRepository.findById(id).orElseThrow();
        if (body.containsKey("active")) c.setActive(Boolean.parseBoolean(body.get("active").toString()));
        return cityRepository.save(c);
    }

    @PostMapping("/areas")
    public Area createArea(@RequestBody Map<String, Object> body) {
        Area a = new Area();
        a.setCityId(Long.valueOf(body.get("cityId").toString()));
        a.setName(body.get("name").toString());
        return areaRepository.save(a);
    }

    @PostMapping("/estimates")
    public CategoryCityEstimate createEstimate(@RequestBody Map<String, Object> body) {
        CategoryCityEstimate e = new CategoryCityEstimate();
        e.setCategoryId(Long.valueOf(body.get("categoryId").toString()));
        e.setCityId(Long.valueOf(body.get("cityId").toString()));
        e.setHoursChoice(com.supplybase.partners.partner.WorkingHoursChoice.valueOf(body.get("hoursChoice").toString()));
        e.setEstimatedMonthlyPaise(Long.parseLong(body.get("estimatedMonthlyPaise").toString()));
        e.setAssumptionsText(body.get("assumptionsText").toString());
        return estimateRepository.save(e);
    }

    @PostMapping("/policies")
    public PolicyDocument publishPolicy(@RequestBody Map<String, Object> body) {
        PolicyDocument.PolicyType type = PolicyDocument.PolicyType.valueOf(body.get("type").toString());
        policyDocumentRepository.findByTypeAndCurrentTrue(type).ifPresent(prev -> {
            prev.setCurrent(false);
            policyDocumentRepository.save(prev);
        });
        PolicyDocument p = new PolicyDocument();
        p.setType(type);
        p.setVersion(body.get("version").toString());
        p.setTitle(body.get("title").toString());
        p.setContentMarkdown(body.get("contentMarkdown").toString());
        p.setCurrent(true);
        return policyDocumentRepository.save(p);
    }
}
