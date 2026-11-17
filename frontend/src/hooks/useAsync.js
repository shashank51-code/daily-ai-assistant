import { useCallback, useEffect, useState } from 'react';
import { apiErrorMessage } from '../api/client';

/**
 * Small data-loading hook with explicit loading, error and reload states so every
 * screen can show a real spinner, an error message and a retry button.
 */
export default function useAsync(loader, dependencies = []) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [reloadToken, setReloadToken] = useState(0);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const run = useCallback(loader, dependencies);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);

    run()
      .then((result) => {
        if (active) setData(result);
      })
      .catch((err) => {
        if (active) setError(apiErrorMessage(err));
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [run, reloadToken]);

  const reload = useCallback(() => setReloadToken((value) => value + 1), []);

  return { data, loading, error, reload, setData };
}
