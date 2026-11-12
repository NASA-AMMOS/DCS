import {expect, test, vi} from "vitest";
import {createErrorCallback} from "./api.js";

const run = (e) => {
    const enqueue = vi.fn()
    vi.spyOn(console, "log").mockImplementation(() => {})
    createErrorCallback(enqueue)(e)
    return enqueue.mock.calls[0]
}

test("single server message is shown", () => {
    expect(run({response: {status: 400, data: {messages: ["bad csv"]}}})[0]).toBe("HTTP 400:bad csv")
})

test("multiple messages are summarized", () => {
    expect(run({response: {status: 400, data: {messages: ["a", "b"]}}})[0]).toMatch(/2 error messages/)
})

test("a response with no body or no messages does not throw", () => {
    expect(run({response: {status: 500}})[0]).toBe("HTTP 500:")
    expect(run({response: {status: 500, data: {}}})[0]).toBe("HTTP 500:")
})

test("a network error with no response does not throw", () => {
    expect(run({message: "Network Error"})[0]).toBe("Network Error")
    expect(run({request: {status: 0}})[0]).toMatch(/Request failed/)
})
