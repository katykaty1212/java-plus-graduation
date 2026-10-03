package ru.practicum.compilation;

import lombok.experimental.UtilityClass;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.event.EventMapper;

import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class CompilationMapper {

    public static CompilationDto toCompilationDto(Compilation compilation) {
        if (compilation == null) {
            return null;
        }

        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(compilation.getEvents() == null ? List.of() :
                        compilation.getEvents().stream()
                                .map(EventMapper::toEventShortDto)  // ← теперь метод есть!
                                .collect(Collectors.toList()))
                .build();
    }

    public static List<CompilationDto> toCompilationDtoList(List<Compilation> compilations) {
        if (compilations == null) {
            return List.of();
        }
        return compilations.stream()
                .map(CompilationMapper::toCompilationDto)
                .collect(Collectors.toList());
    }
}