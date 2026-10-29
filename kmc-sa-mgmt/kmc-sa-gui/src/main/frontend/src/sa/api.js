/*
 * Copyright (c) 2014-26 California Institute of Technology (“Caltech”). U.S. Government sponsorship acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 *  • Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *  • Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *  • Neither the name of Caltech nor its operating division, the Jet Propulsion Laboratory, nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Open Source License Approved by Caltech/JPL
 * APACHE LICENSE, VERSION 2.0
 *  • Text version: https://www.apache.org/licenses/LICENSE-2.0.txt
 *  • SPDX short identifier: Apache-2.0
 *  • OSI Approved License: https://opensource.org/licenses/Apache-2.0
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
