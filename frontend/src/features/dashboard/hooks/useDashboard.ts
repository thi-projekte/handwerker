import { useEffect, useState } from "react";
import { getDashboardStats, DashboardStatsResponse } from "@/data/api/dashboardApi";

interface UseDashboardState {
  data: DashboardStatsResponse | null;
  loading: boolean;
  error: Error | null;
}

export const useDashboard = () => {
  const [state, setState] = useState<UseDashboardState>({
    data: null,
    loading: true,
    error: null,
  });

  useEffect(() => {
    const loadDashboard = async () => {
      try {
        setState((prev) => ({ ...prev, loading: true, error: null }));
        const stats = await getDashboardStats();
        setState({ data: stats, loading: false, error: null });
      } catch (err) {
        setState({
          data: null,
          loading: false,
          error: err instanceof Error ? err : new Error("Unknown error"),
        });
      }
    };

    loadDashboard();
  }, []);

  return state;
};