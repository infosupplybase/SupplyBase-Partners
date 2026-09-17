package com.supplybase.partners.kit;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/catalog/starter-kits")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStarterKitCatalogController {

    private final StarterKitRepository starterKitRepository;
    private final StarterKitItemRepository starterKitItemRepository;

    public AdminStarterKitCatalogController(StarterKitRepository starterKitRepository, StarterKitItemRepository starterKitItemRepository) {
        this.starterKitRepository = starterKitRepository;
        this.starterKitItemRepository = starterKitItemRepository;
    }

    @PostMapping
    @SuppressWarnings("unchecked")
    public StarterKit create(@RequestBody Map<String, Object> body) {
        StarterKit kit = new StarterKit();
        kit.setCategoryId(Long.valueOf(body.get("categoryId").toString()));
        kit.setName(body.get("name").toString());
        kit.setDescription((String) body.get("description"));
        kit.setPricePaise(Long.parseLong(body.get("pricePaise").toString()));
        kit.setFeesPaise(body.containsKey("feesPaise") ? Long.parseLong(body.get("feesPaise").toString()) : 0);
        kit.setTermsText(body.get("termsText").toString());
        starterKitRepository.save(kit);

        List<String> items = (List<String>) body.get("items");
        if (items != null) {
            for (String itemName : items) {
                StarterKitItem item = new StarterKitItem();
                item.setKitId(kit.getId());
                item.setName(itemName);
                starterKitItemRepository.save(item);
            }
        }
        return kit;
    }
}
