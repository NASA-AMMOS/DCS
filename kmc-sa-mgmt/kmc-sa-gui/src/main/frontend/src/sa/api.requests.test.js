import fs from 'node:fs'
import path from 'node:path'
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import axios from 'axios'
import * as api from './api'

vi.mock('axios', () => ({default: vi.fn()}))

// vitest runs from the frontend root (src/main/frontend)
const CONTROLLER = path.resolve(process.cwd(), '../java/gov/nasa/jpl/ammos/asec/kmc/saserver/app/sa/SaController.java')

/** [METHOD, path template] for every route SaController declares, e.g. ['POST', '/api/sa/iv/{type}'] */
const controllerRoutes = () => {
    const src = fs.readFileSync(CONTROLLER, 'utf8')
    const prefix = src.match(/@RequestMapping\("([^"]+)"\)/)[1]
    return [...src.matchAll(/@(Get|Post|Put|Delete)Mapping\(([^)]*)\)/g)].flatMap(([, method, args]) =>
        [...args.matchAll(/"(\/[^"]*)"/g)].map(([, path]) => [method.toUpperCase(), prefix + path]))
}

const routeExists = (routes, method, url) => routes.some(([m, template]) => m === method &&
    new RegExp('^' + template.replace(/\{[^}]+}/g, '[^/]+') + '$').test('/' + url))

const ID = {spi: 1, scid: 46}

// every call that targets one frame type, with the request it must send for TM
const calls = [
    ['listSa', () => api.listSa('TM', vi.fn(), vi.fn()), 'GET', 'api/sa/TM'],
    ['createSa', () => api.createSa('TM', {spi: 1, scid: 46}, vi.fn(), vi.fn()), 'PUT', 'api/sa', {type: 'TM'}],
    ['updateSa', () => api.updateSa('TM', {spi: 1}, vi.fn(), vi.fn()), 'POST', 'api/sa/TM'],
    ['startSa', () => api.startSa('TM', ID, vi.fn(), vi.fn()), 'POST', 'api/sa/start/TM', ID],
    ['stopSa', () => api.stopSa('TM', ID, vi.fn(), vi.fn()), 'POST', 'api/sa/stop/TM', ID],
    ['expireSa', () => api.expireSa('TM', ID, vi.fn(), vi.fn()), 'POST', 'api/sa/expire/TM', ID],
    ['deleteSa', () => api.deleteSa('TM', ID, vi.fn(), vi.fn()), 'DELETE', 'api/sa/TM', [ID]],
    ['deleteSas', () => api.deleteSas('TM', [ID], vi.fn(), vi.fn()), 'DELETE', 'api/sa/TM', [ID]],
    ['resetArsn', () => api.resetArsn('TM', ID, '01', vi.fn(), vi.fn()), 'POST', 'api/sa/arsn/TM'],
    ['modifyArsn', () => api.modifyArsn('TM', {id: ID}, vi.fn(), vi.fn()), 'POST', 'api/sa/arsn/TM'],
    ['modifyIv', () => api.modifyIv('TM', {id: ID}, vi.fn(), vi.fn()), 'POST', 'api/sa/iv/TM'],
    ['rekeySa', () => api.rekeySa('TM', {id: ID}, vi.fn(), vi.fn()), 'POST', 'api/sa/key/TM'],
    ['bulkCreate', () => api.bulkCreate(['TM', 'TC'], new FormData(), vi.fn(), vi.fn(), vi.fn()), 'POST',
        'api/sa/create/TM,TC'],
]

describe('api request contract', () => {
    beforeEach(() => {
        axios.mockResolvedValue({status: 200, data: []})
        vi.spyOn(console, 'log').mockImplementation(() => {})
    })
    afterEach(() => vi.restoreAllMocks())

    // axios is called either as axios(url) or axios({method, url, data})
    const sent = () => {
        const [arg] = axios.mock.calls.at(-1)
        return typeof arg === 'string' ? {method: 'GET', url: arg} : {...arg, method: arg.method.toUpperCase()}
    }

    it.each(calls)('%s sends the frame type to the right endpoint', async (name, call, method, url, data) => {
        await call()
        const req = sent()
        expect(req.method).toBe(method)
        expect(req.url).toBe(url)
        if (data !== undefined) {
            expect(req.data).toMatchObject(data)
        }
    })

    it.each([...calls, ['downloadCsv', () => api.downloadCsv(vi.fn(), vi.fn())],
        ['status', () => api.status(vi.fn(), vi.fn())]])('%s targets a route SaController declares', async (name, call) => {
        await call()
        const {method, url} = sent()
        expect(routeExists(controllerRoutes(), method, url), `${method} /${url}`).toBe(true)
    })

    it('reads the controller routes', () => {
        expect(controllerRoutes()).toContainEqual(['POST', '/api/sa/iv/{type}'])
    })
})
