function fetchWithTimeout(operation) {
  const FETCH_TIMEOUT = 5000;
  let didTimeOut = false;

  return new Promise(function(resolve, reject) {
    const timeout = setTimeout(function() {
      didTimeOut = true;
      reject(new Error('Request timed out'))
    }, FETCH_TIMEOUT);

    operation()
      .then(function(response) {
        // Clear the timeout as cleanup
        clearTimeout(timeout);
        if (!didTimeOut) {
          resolve(response)
        }
      })
      .catch(function(err) {
        console.log('fetch failed! ', err);

        // Rejection already happened with setTimeout
        if (didTimeOut) return;
        // Reject with error
        reject(err)
      })
  })
}
