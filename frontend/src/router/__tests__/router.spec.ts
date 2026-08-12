import { describe, expect, it } from 'vitest'
import router, { routes } from '../index'

describe('workspace routes', () => {
  it('provides titled routes and redirects unknown locations home', () => {
    const paths = router.getRoutes().map((route) => route.path)
    expect(paths).toEqual(expect.arrayContaining(['/', '/library', '/reader/:id', '/recent', '/favorites', '/notes', '/review', '/ai', '/settings']))
    expect(router.getRoutes().filter((route) => route.path !== '/' && route.path !== '/:pathMatch(.*)*').every((route) => typeof route.meta?.title === 'string')).toBe(true)
    expect(routes[routes.length - 1]?.redirect).toBe('/')
  })
})
