import { useEffect, useRef } from "react";
import { useLocation, useNavigate } from "react-router-dom";

const swipeRoutes = ["/dashboard", "/angebote", "/home", "/unternehmen", "/profil"];

const MIN_SWIPE_DISTANCE = 45;
const MAX_VERTICAL_DISTANCE = 80;
const ROUTE_CHANGE_PERCENTAGE = 0.08;

export const useSwipeNavigation = (enabled = true) => {
  const navigate = useNavigate();
  const location = useLocation();

  const touchStartX = useRef(0);
  const touchStartY = useRef(0);
  const isHorizontalSwipe = useRef(false);
  const currentTranslate = useRef(0);

  useEffect(() => {
    if (!enabled) {
      return;
    }

    const contentElement = document.querySelector<HTMLElement>(".shell-content");

    if (!contentElement) {
      return;
    }

    const resetContentPosition = () => {
      contentElement.classList.add("swipe-reset");
      contentElement.style.transform = "translateX(0)";
      contentElement.style.opacity = "1";

      window.setTimeout(() => {
        contentElement.classList.remove("swipe-reset");
      }, 180);
    };

    const isInteractiveElement = (target: EventTarget | null) => {
      if (!(target instanceof HTMLElement)) {
        return false;
      }

      return Boolean(
        target.closest("button, input, textarea, select, a, label"),
      );
    };

    const handleTouchStart = (event: TouchEvent) => {
      if (isInteractiveElement(event.target)) {
        return;
      }

      touchStartX.current = event.touches[0].clientX;
      touchStartY.current = event.touches[0].clientY;
      isHorizontalSwipe.current = false;
      currentTranslate.current = 0;

      contentElement.classList.remove("swipe-reset");
    };

    const handleTouchMove = (event: TouchEvent) => {
      if (isInteractiveElement(event.target)) {
        return;
      }

      const touchCurrentX = event.touches[0].clientX;
      const touchCurrentY = event.touches[0].clientY;

      const horizontalDistance = touchCurrentX - touchStartX.current;
      const verticalDistance = Math.abs(touchCurrentY - touchStartY.current);

      if (
        Math.abs(horizontalDistance) > MIN_SWIPE_DISTANCE &&
        verticalDistance < MAX_VERTICAL_DISTANCE
      ) {
        isHorizontalSwipe.current = true;
      }

      if (!isHorizontalSwipe.current) {
        return;
      }

      event.preventDefault();

      const currentIndex = swipeRoutes.indexOf(location.pathname);

      if (currentIndex === -1) {
        return;
      }

      const isSwipeLeft = horizontalDistance < 0;
      const isSwipeRight = horizontalDistance > 0;

      const isFirstPage = currentIndex === 0;
      const isLastPage = currentIndex === swipeRoutes.length - 1;

      if ((isSwipeRight && isFirstPage) || (isSwipeLeft && isLastPage)) {
        currentTranslate.current = horizontalDistance * 0.15;
      } else {
        currentTranslate.current = horizontalDistance * 0.8;
      }

      const dragProgress = Math.min(
        Math.abs(currentTranslate.current) / window.innerWidth,
        0.18,
      );

      contentElement.style.transform = `translateX(${currentTranslate.current}px)`;
      contentElement.style.opacity = `${1 - dragProgress}`;
    };

    const handleTouchEnd = () => {
      if (!isHorizontalSwipe.current) {
        resetContentPosition();
        return;
      }

      const currentIndex = swipeRoutes.indexOf(location.pathname);

      if (currentIndex === -1) {
        resetContentPosition();
        return;
      }

      const threshold = window.innerWidth * ROUTE_CHANGE_PERCENTAGE;
      const shouldChangeRoute = Math.abs(currentTranslate.current) >= threshold;

      if (!shouldChangeRoute) {
        resetContentPosition();
        return;
      }

      const nextIndex =
        currentTranslate.current < 0 ? currentIndex + 1 : currentIndex - 1;

      if (nextIndex < 0 || nextIndex >= swipeRoutes.length) {
        resetContentPosition();
        return;
      }

      contentElement.classList.add("swipe-reset");
      contentElement.style.transform = "translateX(0)";
      contentElement.style.opacity = "1";

      navigate(swipeRoutes[nextIndex]);
    };

    window.addEventListener("touchstart", handleTouchStart, { passive: true });
    window.addEventListener("touchmove", handleTouchMove, { passive: false });
    window.addEventListener("touchend", handleTouchEnd, { passive: true });

    return () => {
      window.removeEventListener("touchstart", handleTouchStart);
      window.removeEventListener("touchmove", handleTouchMove);
      window.removeEventListener("touchend", handleTouchEnd);
    };
  }, [enabled, location.pathname, navigate]);
};