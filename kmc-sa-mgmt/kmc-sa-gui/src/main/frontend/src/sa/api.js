/*
 * Copyright 2026, by the California Institute of Technology.
 * ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology
 * Transfer at the California Institute of Technology.
 */
/**
 * Backend API calls
 *
 * @author panjames
 */
import axios from "axios";

export const listSa = async (type, respCallback, errCallback) => {
    try {
        const result = await axios("api/sa/" + type)
        respCallback(result)
    } catch (err) {
        errCallback(err)
    }
}

/**
 * Create an SA
 * @param type frame type (TC/TM/AOS)
 * @param values SA in JSON
 * @param respCallback response callback
 * @param errCallback error callback
 */
export const createSa = (type, values, respCallback, errCallback) => {
    console.log('creating spi/scid: ' + values.spi + '/' + values.scid)
    values.type = type
    axios({
        method: 'put',
        url: "api/sa",
        data: values
    }).then(response => {
        respCallback(response)
    }).catch(e => {
        errCallback(e)
    })
}

/**
 * Start an SA
 * @param type frame type (TC/TM/AOS)
 * @param id SA ID (SPI/SCID)
 * @param respCallback response callback
 * @param errCallback error callback
 */
export const startSa = (type, id, respCallback, errCallback) => {
    console.log(`starting ${id.spi}/${id.scid}`)
    axios({
        method: 'post',
        url: 'api/sa/start/' + type,
        data: id
    }).then(resp => {
        respCallback(resp)
    }).catch(e => {
        errCallback(e)
    })
}

/**
 * Stop an SA
 * @param type frame type (TC/TM/AOS)
 * @param id SA ID (SPI/SCID)
 * @param respCallback response callback
 * @param errCallback error callback
 */
export const stopSa = (type, id, respCallback, errCallback) => {
    console.log(`starting ${type} ${id.spi}/${id.scid}`)
    axios({
        method: 'post',
        url: `api/sa/stop/${type}`,
        data: id
    }).then(resp => {
        respCallback(resp)
    }).catch(e => {
        errCallback(e)
    })
}

/**
 * Update an SA
 * @param type frame type (TC/TM/AOS)
 * @param values SA in JSON
 * @param respCallback response callback
 * @param errCallback error callback
 */
export const updateSa = (type, values, respCallback, errCallback) => {
    console.log(`updating ${type} spi/scid: ${values.spi}/${values.scid}`)
    axios({
        method: 'post',
        url: "api/sa/" + type,
        data: values
    }).then(resp => {
        respCallback(resp)
    }).catch(e => {
        errCallback(e)
    })
}

/**
 * Delete an SA
 * @param type frame type (TC/TM/AOS)
 * @param id SA ID (SPI/SCID)
 * @param respCallback response callback
 * @param errCallback error callback
 */
export const deleteSa = (type, id, respCallback, errCallback) => {
    console.log(`deleting ${type} spi/scid: ${id.spi}/${id.scid}`)
    axios({
        method: 'delete',
        url: `api/sa/${type}`,
        data: [id]
    }).then(resp => {
        respCallback(resp)
    }).catch(e => {
        errCallback(e)
    })
}

/**
 * Delete multiple SAs
 * @param type frame type (TC/TM/AOS)
 * @param ids array of SA IDs (SPI/SCID)
 * @param respCallback
 * @param errCallback
 */
export const deleteSas = (type, ids, respCallback, errCallback) => {
    console.log(`deleting multiple ${type} SAs`)
    axios({
        method: 'delete',
        url: `api/sa/${type}`,
        data: ids
    }).then(resp => {
        respCallback(resp)
    }).catch(e => {
        errCallback(e)
    })
}

/**
 * Expire an SA
 * @param id SA ID (SPI/SCID)
 * @param respCallback response callback
 * @param errCallback error callback
 */
export const expireSa = (type, id, respCallback, errCallback) => {
    console.log(`expiring ${type} spi/scid: ${id.spi}/${id.scid}`)
    axios({
        method: 'post',
        url: `api/sa/expire/${type}`,
        data: id
    }).then(resp => {
        respCallback(resp)
    }).catch(e => {
        errCallback(e)
    })
}

/**
 * Reset ARSN
 * @param type frame type (TC/TM/AOS)
 * @param id
 * @param arsn
 * @param respCallback
 * @param errCallback
 */
export const resetArsn = (type, id, arsn, respCallback, errCallback) => {
    console.log(`resetting arsn to ${arsn} on ${type} spi/scid: ${id.spi} / ${id.scid}`)
    axios({
        method: 'post',
        url: `api/sa/arsn/${type}`,
        data: {
            id: id,
            arsn: arsn
        }
    }).then(resp => {
        respCallback(resp)
    }).catch(e => {
        errCallback(e)
    })
}

/**
 * Create SAs via CSV upload
 * @param type CSV of frame types (eg, "TC,TM,AOS")
 * @param data form data
 * @param setProgress progress function
 * @param respCallback response callback
 * @param errCallback error callback
 */
export const bulkCreate = (type, data, setProgress, respCallback, errCallback) => {
    console.log(`creating multiple SAs via CSV upload`)
    axios({
        method: 'post',
        headers: {
            'Content-Type': 'multipart/form-data'
        },
        url: `api/sa/create/${type}`,
        data: data,
        onUploadProgress: (progressEvent) => {
            const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
            setProgress(progress)
        }
    }).then(r => {
        respCallback(r)
    }).catch(e => {
        errCallback(e)
    })
}

/**
 * Dowload SADB CSV
 * @param respCallback response callback
 * @param errCallback error callback
 */
export const downloadCsv = (respCallback, errCallback) => {
    console.log('downloading sadb csv')
    axios({
        method: 'get',
        url: 'api/sa/csv',
        responseType: 'blob'
    }).then(r => {
        respCallback(r)
    }).catch(e => {
        errCallback(e)
    })
}

/**
 * Create a response callback
 * @param msg snackbar message
 * @param onClose close action
 * @param refresh refresh table
 * @param enqueueSnackbar snackbar queue
 * @returns {(function(*): void)|*} response callback
 */
export const createResponseCallback = (msg, onClose, refresh, enqueueSnackbar) => {
    return (resp) => {
        if (resp.status >= 200 && resp.status < 300) {
            onClose()
            refresh()
            enqueueSnackbar(msg, {variant: "success"})
        }
    }
}

/**
 * Create an error callback
 * @param enqueueSnackbar snackbar queue
 * @returns {(function(*): void)|*} error callback
 */
export const createErrorCallback = (enqueueSnackbar) => {
    return (e) => {
        let txt
        if (e.response) {
            txt = `HTTP ${e.response.status}:`
            if (e.response.data.messages.length > 1) {
                txt += `${e.response.data.messages.length} error messages received, see console for details`
            } else {
                e.response.data.messages.forEach(m => txt += m)
            }
        } else if (e.request) {
            txt = `HTTP ${e.request.status}: Request failed`
        } else {
            txt = `${e.message}`
        }
        console.log(e)
        console.log(txt)
        console.log(e.response.data.messages)
        enqueueSnackbar(txt, {variant: "error"})
    }
}

/**
 * Modify ARSN
 * @param type frame type (TC/TM/AOS)
 * @param data spi/scid id, arsn, arsnLen, arswn
 * @param respCallback response callback
 * @param errCallback error callback
 */
export const modifyArsn = (type, data, respCallback, errCallback) => {
    axios({
        method: 'post',
        url: `api/sa/arsn/${type}`,
        data: data
    }).then((r) => {
        respCallback(r)
    }).catch((e) => {
        errCallback(e)
    })
}

/**
 * Modify IV
 * @param type frame type (TC/TM/AOS)
 * @param data spi/scid, iv, ivLen
 * @param respCallback response callback
 * @param errCallback error callback
 */
export const modifyIv = (type, data, respCallback, errCallback) => {
    axios({
        method: 'post',
        url: `api/sa/iv/${type}`,
        data: data
    }).then((r) => {
        respCallback(r)
    }).catch((e) => {
        errCallback(e)
    })
}

/**
 * Rekey SA
 * @param type frame type (TC/TM/AOS)
 * @param data spi/scid id, ekid, akid
 * @param respCallback response callback
 * @param errCallback error callback
 */
export const rekeySa = (type, data, respCallback, errCallback) => {
    axios({
        method: 'post',
        url: `api/sa/key/${type}`,
        data: data
    }).then(r => {
        respCallback(r)
    }).catch(e => {
        errCallback(e)
    })
}

export const status = (respCallback, errCallback) => {
    axios({
        method: 'get',
        url: 'api/status'
    }).then(r => {
        respCallback(r)
    }).catch(e => {
        errCallback(e)
    })
}
