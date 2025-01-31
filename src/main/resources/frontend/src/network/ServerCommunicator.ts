import { useAuthStore } from "@/stores/auth";
import { ServerError } from "@/network/ServerError";
import { useConfigStore } from "@/stores/config";

/**
 * Utility for making authenticated HTTP requests to the server with automatic error handling
 * and response parsing.
 *
 * @example
 * // GET request expecting a User response
 * const user = await ServerCommunicator.getRequest<User>('/api/user');
 *
 * // POST request with body, not expecting response
 * await ServerCommunicator.postRequest<void>('/api/logs', { event: 'action' }, false);
 */
export const ServerCommunicator = {
  getRequest: getRequest,
  getRequestGuaranteed: getRequestGuaranteed,
  postRequest: postRequest,
  doUnprocessedRequest: doUnprocessedRequest,
};

/**
 * Makes a GET request to the specified endpoint with a guaranteed response.
 * This will not throw an error if the server returns an error, but will print it
 * to the console.
 * @template T - The type of the expected response (when expectResponse is true)
 * @param {string} endpoint - The API endpoint to call
 * @param {T} errorResponse - The object the method call should return if the server
 * returns nothing or responds with a non-2XX code
 * @returns {Promise<T>} Promise that resolves to the response data of type T
 */
function getRequestGuaranteed<T>(endpoint: string, errorResponse: T): Promise<T> {
  return getRequest<T>(endpoint, true).catch((_error) => Promise.resolve(errorResponse));
}

/**
 * Makes a GET request to the specified endpoint.
 * @template T - The type of the expected response (when expectResponse is true)
 * @param {string} endpoint - The API endpoint to call
 * @param {boolean} [expectResponse=true] - Whether to expect and parse a response
 * @returns {Promise<T | null>} Promise that resolves to:
 *   - The response data of type T when expectResponse is true
 *   - null when expectResponse is false
 * @throws {ServerError} When the request fails (meaning the server returned a code other than 2XX)
 * @throws {Error} when expectResponse is true but no response is received
 */
function getRequest(endpoint: string, expectResponse: false): Promise<null>;
/**
 * Makes a GET request to the specified endpoint.
 * @template T - The type of the expected response (when expectResponse is true)
 * @param {string} endpoint - The API endpoint to call
 * @param {boolean} [expectResponse=true] - Whether to expect and parse a response
 * @returns {Promise<T | null>} Promise that resolves to:
 *   - The response data of type T when expectResponse is true
 *   - null when expectResponse is false
 * @throws {ServerError} When the request fails (meaning the server returned a code other than 2XX)
 * @throws {Error} when expectResponse is true but no response is received
 */
function getRequest<T>(endpoint: string, expectResponse?: boolean): Promise<T>;
function getRequest<T>(endpoint: string, expectResponse: boolean = true): Promise<T | null> {
  return doRequest("GET", endpoint, null, expectResponse);
}

/**
 * Makes a POST request to the specified endpoint.
 * @template T - The type of the expected response (when expectResponse is true)
 * @param {string} endpoint - The API endpoint to call
 * @param {Object | null} [bodyObject=null] - The request body object to send (will be sent as JSON)
 * @param {boolean} [expectResponse=true] - Whether to expect and parse a response
 * @returns {Promise<T | null>} Promise that resolves to:
 *   - The response data of type T when expectResponse is true
 *   - null when expectResponse is false
 * @throws {ServerError} When the request fails (meaning the server returned a code other than 2XX)
 * @throws {Error} when expectResponse is true but no response is received
 *
 * @example
 * // With response
 * const user = await postRequest<User>('/api/users', { name: 'John' });
 *
 * // Without response
 * await postRequest<void>('/api/logs', { event: 'action' }, false);
 */
function postRequest(
  endpoint: string,
  bodyObject: Object | null,
  expectResponse: false,
): Promise<null>;
/**
 * Makes a POST request to the specified endpoint.
 * @template T - The type of the expected response (when expectResponse is true)
 * @param {string} endpoint - The API endpoint to call
 * @param {Object | null} [bodyObject=null] - The request body object to send (will be sent as JSON)
 * @param {boolean} [expectResponse=true] - Whether to expect and parse a response
 * @returns {Promise<T | null>} Promise that resolves to:
 *   - The response data of type T when expectResponse is true
 *   - null when expectResponse is false
 * @throws {ServerError} When the request fails (meaning the server returned a code other than 2XX)
 * @throws {Error} when expectResponse is true but no response is received
 *
 * @example
 * // With response
 * const user = await postRequest<User>('/api/users', { name: 'John' });
 *
 * // Without response
 * await postRequest<void>('/api/logs', { event: 'action' }, false);
 */
function postRequest<T>(
  endpoint: string,
  bodyObject?: Object | null,
  expectResponse?: boolean,
): Promise<T>;
function postRequest<T>(
  endpoint: string,
  bodyObject: Object | null = null,
  expectResponse: boolean = true,
): Promise<T | null> {
  return doRequest<T>("POST", endpoint, bodyObject, expectResponse);
}
/**
 * Internal method to make an HTTP request.
 * @template T - The type of the expected response (when expectResponse is true)
 * @param {string} method - The HTTP method to use
 * @param {string} endpoint - The API endpoint to call
 * @param {Object | null} [bodyObject=null] - The request body object to send (will be sent as JSON)
 * @param {boolean} [expectResponse=true] - Whether to expect and parse a response
 * @returns {Promise<T | null>} Promise that resolves to:
 *   - The response data of type T when expectResponse is true
 *   - null when expectResponse is false
 * @throws {ServerError} When the request fails (meaning the server returned a code other than 2XX)
 * @throws {Error} When expectResponse is true but no response is received
 * @internal
 */
function doRequest(
  method: string,
  endpoint: string,
  bodyObject: Object | null,
  expectResponse: false,
): Promise<null>;
/**
 * Internal method to make an HTTP request.
 * @template T - The type of the expected response (when expectResponse is true)
 * @param {string} method - The HTTP method to use
 * @param {string} endpoint - The API endpoint to call
 * @param {Object | null} [bodyObject=null] - The request body object to send (will be sent as JSON)
 * @param {boolean} [expectResponse=true] - Whether to expect and parse a response
 * @returns {Promise<T | null>} Promise that resolves to:
 *   - The response data of type T when expectResponse is true
 *   - null when expectResponse is false
 * @throws {ServerError} When the request fails (meaning the server returned a code other than 2XX)
 * @throws {Error} When expectResponse is true but no response is received
 * @internal
 */
function doRequest<T>(
  method: string,
  endpoint: string,
  bodyObject?: Object | null,
  expectResponse?: boolean,
): Promise<T>;
/**
 * Internal method to make an HTTP request.
 * @template T - The type of the expected response (when expectResponse is true)
 * @param {string} method - The HTTP method to use
 * @param {string} endpoint - The API endpoint to call
 * @param {Object | null} [bodyObject=null] - The request body object to send (will be sent as JSON)
 * @param {boolean} [expectResponse=true] - Whether to expect and parse a response
 * @returns {Promise<T | null>} Promise that resolves to:
 *   - The response data of type T when expectResponse is true
 *   - null when expectResponse is false
 * @throws {ServerError} When the request fails (meaning the server returned a code other than 2XX)
 * @throws {Error} When expectResponse is true but no response is received
 * @internal
 */
async function doRequest<T>(
  method: string,
  endpoint: string,
  bodyObject: Object | null = null,
  expectResponse: boolean = true,
): Promise<T | null> {
  const response = await doUnprocessedRequest(method, endpoint, bodyObject);

  if (!expectResponse) {
    return null;
  }

  const text = await response.text();
  if (text) {
    return JSON.parse(text) as T;
  }

  if (bodyObject) {
    console.error("Body request:", bodyObject);
  }
  console.error("Response: ", response);
  throw new Error(`Expected a response from ${method} call to ${endpoint} but got none`);
}

/**
 * Makes a raw HTTP request to the server with authentication.
 * @param {string} method - The HTTP method to use
 * @param {string} endpoint - The API endpoint to call
 * @param {Object | null} [bodyObject=null] - The request body object to send (will be sent as JSON)
 * @returns {Promise<Response>} A promise that resolves to the raw fetch response object
 * @throws {ServerError} When the request fails (meaning the server returned a code other than 2XX)
 */
async function doUnprocessedRequest(
  method: string,
  endpoint: string,
  bodyObject: Object | null = null,
): Promise<Response> {
  const authToken = useAuthStore().token ?? "";

  const response = await fetch(useConfigStore().backendUrl + endpoint, {
    method: method,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      Authorization: authToken,
    },
    body: bodyObject ? JSON.stringify(bodyObject) : null,
  });

  if (!response.ok) {
    console.error(
      `A ${response.status} error occurred while making a ${method} request to ${endpoint}`,
    );
    console.error(response);
    throw new ServerError(endpoint, await response.text(), response.status, response.statusText);
  }
  return response;
}
