package com.silhouettepuzzle.geometry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ModelValidationTest {

    @Test
    void outlineComputesItsArea() {
        assertThat(Outline.of(0, 0, 4, 0, 4, 3, 0, 3).area()).isCloseTo(12, within(1e-9));
    }

    @Test
    void outlineRejectsFewerThanThreeVertices() {
        assertThatThrownBy(() -> Outline.of(0, 0, 1, 1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void outlineRejectsASelfIntersectingBowtie() {
        assertThatThrownBy(() -> Outline.of(0, 0, 2, 2, 2, 0, 0, 2)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void outlineRejectsCollinearVertices() {
        assertThatThrownBy(() -> Outline.of(0, 0, 1, 1, 2, 2)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void outlineRejectsAnOddNumberOfCoordinates() {
        assertThatThrownBy(() -> Outline.of(0, 0, 1, 0, 1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pointRejectsNonFiniteCoordinates() {
        assertThatThrownBy(() -> new Point(Double.NaN, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Point(0, Double.POSITIVE_INFINITY)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {-45, 30, 360, 405})
    void placementRejectsRotationsOffTheStep(int rotation) {
        assertThatThrownBy(() -> new Placement(0, 0, rotation, false)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 45, 90, 135, 180, 225, 270, 315})
    void placementAcceptsEveryStep(int rotation) {
        assertThat(new Placement(0, 0, rotation, false).rotation()).isEqualTo(rotation);
    }

    @Test
    void placementRejectsANonFinitePosition() {
        assertThatThrownBy(() -> new Placement(Double.NaN, 0, 0, false)).isInstanceOf(IllegalArgumentException.class);
    }
}
