# Design System Strategy: High-Performance Gaming Utility

## 1. Overview & Creative North Star
**Creative North Star: The Kinetic Command Center**

This design system is built to evoke the feeling of a high-end, responsive tactical interface. It moves away from the "static web" aesthetic toward a "living software" experience. We achieve this by breaking the traditional rigid grid through **Kinetic Layering**—using varying depths of glassmorphism and intentional asymmetry to guide the user’s eye to high-performance data.

The system is designed for power users who demand speed. By utilizing high-contrast neon accents against a midnight canvas, we create an environment where information doesn't just sit on the screen; it vibrates with potential energy.

## 2. Colors & Surface Philosophy

The color palette is rooted in a "Deep Space" foundation, using light as the primary tool for interaction.

### Surface Hierarchy & Nesting
Instead of using lines to separate modules, we use the **Material Surface Tiers**.
*   **Background (`#0a0f14`):** The base layer. All primary layout containers sit on this.
*   **Surface-Container-Low:** Used for secondary panels or the sidebar background.
*   **Surface-Container-Highest:** Used for active interactive cards or "Smart Boost" modules to create a physical sense of elevation.

### The "No-Line" Rule
**Explicit Instruction:** Do not use 1px solid borders for sectioning content. Boundaries must be defined by:
1.  **Tonal Shifts:** Placing a `surface-container-high` card on a `background` surface.
2.  **Shadow Depth:** Using diffused ambient glows to lift a component.
3.  **Glassmorphism:** Using a semi-transparent surface with a `backdrop-blur` of 12px-20px to create a "frosted" separation.

### Signature Textures
*   **The Power Gradient:** Main CTAs (like "Optimize") must use a linear gradient from `primary` (`#aaffdc`) to `primary_container` (`#00fdc1`).
*   **The Glow State:** Active navigation items and progress rings should utilize an outer glow using the `primary` color at 20% opacity to mimic a hardware LED.

## 3. Typography
We utilize a dual-font approach to balance technical precision with modern editorial flair.

*   **Display & Headlines (Space Grotesk):** This font’s geometric quirks provide a futuristic, "NASA-spec" feel. Use `display-lg` for performance scores and `headline-sm` for module titles.
*   **Body & Labels (Manrope):** A highly legible sans-serif. Used for settings descriptions and technical data points.
*   **Hierarchy Note:** Always lead with high contrast. A `display-lg` score should be paired with a `label-sm` unit (e.g., "77" in Space Grotesk next to "%" in Manrope) to emphasize the data.

## 4. Elevation & Depth

### The Layering Principle
Depth is achieved through stacking. A "Hardware Scan" card should feel like it is floating 10mm above the dashboard. 
*   **Ambient Shadows:** For floating elements, use a blur radius of 32px with a color derived from `on_surface` at 6% opacity. This creates a natural "lift" rather than a dirty smudge.
*   **The "Ghost Border" Fallback:** If a container requires a border (e.g., a card on a dark background), use the `outline_variant` token at **15% opacity**. It should be felt, not seen.

### Glassmorphism
Apply to the sidebar and floating overlays. 
*   **Fill:** `surface_variant` at 40% opacity.
*   **Blur:** 16px backdrop-filter.
*   **Edge:** A top-down 1px gradient stroke (White at 10% to White at 0%) to catch "specular highlights."

## 5. Components

### Buttons
*   **Primary (Action):** Full `primary` gradient with a `on_primary_fixed` label. High-contrast, high-energy.
*   **Secondary (Ghost):** 1px `ghost border` with `primary` text. Use for "Scan" or "Browse" actions.
*   **Tertiary:** No background, `on_surface_variant` text. Used for "Cancel" or "Learn More."

### Circular Score Indicators
The "Score Ring" is the heartbeat of the UI.
*   **Track:** `surface_container_highest`.
*   **Indicator:** `primary` gradient.
*   **Motion:** Use a spring-based animation (Stiffness: 100, Damping: 10) for the score fill.

### Toggle Switches
*   **Unselected:** `surface_container_highest` track with a `outline` thumb.
*   **Selected:** `primary_container` track with a stark white thumb. The track should have a subtle outer glow when active.

### Cards & Lists
**Strict Rule:** No dividers. Separate list items using `spacing-4` (0.9rem) of vertical whitespace. For "System Tools" lists, use a `surface-container-low` background on hover to indicate interactivity.

### Sidebar Navigation
The sidebar uses a "Vertical Indicator" pattern. An active state consists of:
1.  A 4px thick `primary` vertical bar on the far left.
2.  A `surface_variant` background tint.
3.  The icon shifting from `on_surface_variant` to `primary` with a 10% glow.

## 6. Do's and Don'ts

### Do
*   **Do** use asymmetrical layouts (e.g., a large performance card next to two smaller utility cards) to create visual interest.
*   **Do** use color to signify status: `secondary` (Orange) for "Ultimate" states and `tertiary` (Purple) for utility/cleanup.
*   **Do** embrace negative space. Gaming utilities are often cluttered; this system wins through "breathing room" (use `spacing-10` between major modules).

### Don't
*   **Don't** use pure black (`#000000`) for surfaces; use the deep midnight `background` (`#0a0f14`) to maintain depth.
*   **Don't** use standard system icons. Icons must be thin-stroke (1.5pt) and gaming-centric (e.g., a rocket for "Boost," a sword for "Game Mode").
*   **Don't** use sharp 0px corners. Use the `md` (0.375rem) or `lg` (0.5rem) roundedness tokens to keep the futuristic look "sleek" rather than "brutal."