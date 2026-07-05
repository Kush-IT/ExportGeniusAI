package com.exportgenius.ai.config;

import com.exportgenius.ai.entity.Category;
import com.exportgenius.ai.entity.Role;
import com.exportgenius.ai.repository.CategoryRepository;
import com.exportgenius.ai.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final CategoryRepository categoryRepository;

    public DatabaseSeeder(RoleRepository roleRepository, CategoryRepository categoryRepository) {
        this.roleRepository = roleRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        // Seed Roles
        if (roleRepository.count() == 0) {
            roleRepository.save(Role.builder().id(1).name(Role.EXPORTER).build());
            roleRepository.save(Role.builder().id(2).name(Role.ADMIN).build());
            roleRepository.save(Role.builder().id(3).name(Role.IMPORTER).build());
            System.out.println("Default roles seeded successfully on Supabase.");
        }

        // Seed Categories
        if (categoryRepository.count() == 0) {
            categoryRepository.save(Category.builder().id(1).name("Textiles").build());
            categoryRepository.save(Category.builder().id(2).name("Electronics").build());
            categoryRepository.save(Category.builder().id(3).name("Spices").build());
            categoryRepository.save(Category.builder().id(4).name("Handicrafts").build());
            categoryRepository.save(Category.builder().id(5).name("Chemicals").build());
            System.out.println("Default categories seeded successfully on Supabase.");
        }
    }
}
