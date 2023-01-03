package com.alexsobiek.pluginlib.util;

import lombok.Getter;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.ArrayList;
import java.util.List;

@Getter
public class BookBuilder {
    public static int PAGE_LINES = 14;

    protected static int lineCount(Component component) {
        int lines = 0;
        Component newline = Component.newline();
        for (Component c : component.children())
            if (c.equals(newline)) lines++;
        return lines == 0 ? 1 : lines;
    }

    public static SectionBuilder section() {
        return new SectionBuilder();
    }

    private final List<Section> sections = new ArrayList<>();

    public BookBuilder append(Section section) {
        sections.add(section);
        return this;
    }

    public BookBuilder appendStart(Section section) {
        sections.add(0, section);
        return this;
    }

    public Book build() {
        Book.Builder builder = Book.builder();

        sections.forEach(section -> {
            builder.pages(section.pages());
        });

        return builder.build();
    }

    public static class SectionBuilder {
        private final List<Component> body = new ArrayList<>();
        private final List<Component> header = new ArrayList<>();
        private String name;

        public SectionBuilder name(String name) {
            this.name = name;
            return this;
        }

        public SectionBuilder headerLine(Component line) {
            header.add(line);
            return this;
        }

        public SectionBuilder bodyLine(Component line) {
            body.add(line);
            return this;
        }

        public Section build() {
            List<Component> pages = new ArrayList<>();
            int numLines = body.size();
            int freeLines = PAGE_LINES - header.size();

            TextComponent.Builder headerBuilder = Component.text();
            header.forEach(h -> headerBuilder.append(h).append(Component.newline()));

            TextComponent.Builder pageBuilder = Component.text().append(headerBuilder);

            for (int i = 0; i < numLines; i++) {
                if (i != 0 && i % freeLines == 0) { // new page
                    pages.add(pageBuilder.build());
                    pageBuilder = Component.text().append(headerBuilder);
                }
                pageBuilder.append(body.get(i).append(Component.newline()));
            }
            pages.add(pageBuilder.build());
            return new Section(name, headerBuilder.build(), pages);
        }
    }

    public record Section(String name, Component header, List<Component> pages) {
    }
}